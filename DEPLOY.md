# Deploy da Hippo API na Oracle Cloud (Always Free)

Tudo roda numa máquina virtual gratuita, com Docker:

```
Internet ──HTTPS──▶ Caddy (portas 80/443) ──▶ API (8080) ──▶ PostgreSQL
```

- **Caddy:** gera e renova sozinho o certificado HTTPS para o domínio.
- **API e banco:** não ficam expostos para a internet. Só o Caddy fala com a API.
- **Dados do banco:** ficam no volume `postgres_data` e não se perdem ao atualizar a API.

## O que precisa ter antes

| Item | Onde | Vai no `.env` como |
|---|---|---|
| Domínio apontado para o IP da máquina | duckdns.org | `DOMINIO` |
| Chave do Gemini | aistudio.google.com | `GEMINI_API_KEY` |
| Chave da Brevo (acesso restrito: só *Transactional emails*) | brevo.com, em *SMTP & API* | `BREVO_API_KEY` |
| Remetente verificado na Brevo | brevo.com, em *Senders* | `EMAIL_REMETENTE` |

## 1. Criar a máquina virtual

1. Em *Compute → Instances → Create instance*:
   - Imagem: **Ubuntu 24.04**.
   - Shape: **VM.Standard.A1.Flex** com 1 OCPU e 6 GB. Se der "out of capacity", use **VM.Standard.E2.1.Micro**.
   - Em *Add SSH keys*, clique em **Save private key** e guarde o arquivo `.key`.
   - Deixe marcado *Assign a public IPv4 address*.
2. Coloque o **IP público** da máquina no domínio do DuckDNS (campo *current ip* → **update ip**).
3. Libere as portas na Oracle: na página da instância, vá em *Subnet → Security List → Add Ingress Rules* e adicione:
   - origem `0.0.0.0/0`;
   - protocolo TCP;
   - porta de destino `80`;
   - repita para a porta `443`.

## 2. Preparar o servidor

Entre pelo SSH (no Windows, pelo PowerShell ou pelo Git Bash):

```bash
ssh -i caminho/da/chave.key ubuntu@IP_DA_MAQUINA
```

A imagem Ubuntu da Oracle tem um firewall interno que bloqueia as portas mesmo com a regra do passo 1. Libere com:

```bash
sudo iptables -I INPUT 6 -p tcp -m multiport --dports 80,443 -j ACCEPT
sudo netfilter-persistent save
```

Instale o Docker e saia do SSH:

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
exit
```

Depois entre de novo pelo SSH, para a permissão do Docker valer.

**Só se a máquina for a E2.1.Micro (1 GB de RAM):** crie uma memória extra (swap). Sem ela, a compilação da API pode travar.

```bash
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

## 3. Subir a API

```bash
git clone https://github.com/gustavolxma/hippo-api.git
cd hippo-api
cp .env.example .env
nano .env
```

Preencha o `.env`:
- `DB_PASSWORD`: invente uma senha forte.
- `JWT_SECRET`: gere com `openssl rand -base64 48` e cole o resultado.
- As chaves da tabela do início.

No `nano`, salve com `Ctrl+O` e `Enter`, e saia com `Ctrl+X`. Então suba tudo:

```bash
docker compose up -d --build
```

A primeira vez demora alguns minutos, porque compila a API.

## 4. Conferir

- No navegador, abra `https://SEU_DOMINIO/health`. Deve responder `{"status":"UP"}`.
- Para ver os logs da API:

  ```bash
  docker compose logs -f api
  ```

  Saia dos logs com `Ctrl+C`.
- Se o HTTPS não funcionar, veja os logs do Caddy:

  ```bash
  docker compose logs caddy
  ```

  Normalmente a causa é o domínio sem o IP da máquina, ou as portas 80/443 fechadas.
- Se o email não chegar, procure por `Brevo` nos logs da API:
  - erro *"unrecognised IP address"*: cadastre o IP da máquina na Brevo, em *Security → Authorised IPs*;
  - sem erro: olhe também a caixa de spam.

## Atualizar depois de um merge na `main`

```bash
cd hippo-api
git pull
docker compose up -d --build
```

## Comandos úteis

| O quê | Comando |
|---|---|
| Ver o que está rodando | `docker compose ps` |
| Reiniciar só a API | `docker compose restart api` |
| Parar tudo (os dados ficam salvos) | `docker compose down` |
| Backup do banco | `docker compose exec postgres pg_dump -U hippo hippo > backup.sql` |
