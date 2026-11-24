-- O Hibernate (ddl-auto=update) cria colunas novas, mas nao altera as que ja existem.
-- Estes ajustes podem rodar a cada inicio da API sem efeito colateral.

-- A data da Tarefa passou a ser opcional
ALTER TABLE tarefas ALTER COLUMN data_evento DROP NOT NULL;
-- Descricoes de ate 2000 caracteres
ALTER TABLE tarefas ALTER COLUMN descricao SET DATA TYPE varchar(2000);
