CREATE TABLE cliente (
    codigo SERIAL NOT NULL,
    nome VARCHAR(80) NOT NULL,
    tipo_pessoa VARCHAR(15) NOT NULL,
    cpf_cnpj VARCHAR(30),
    telefone VARCHAR(20),
    email VARCHAR(50) NOT NULL,
    logradouro VARCHAR(50),
    numero VARCHAR(15),
    complemento VARCHAR(20),
    cep VARCHAR(15),
    codigo_cidade INTEGER,
    PRIMARY KEY (codigo),
    FOREIGN KEY (codigo_cidade) REFERENCES cidade(codigo)
);