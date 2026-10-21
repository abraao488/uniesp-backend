CREATE TABLE ordens_servico
(
    id                 BIGSERIAL   NOT NULL,
    numero_protocolo   VARCHAR(30) NOT NULL,
    cliente_id         BIGINT      NOT NULL,
    equipamento_id     BIGINT      NOT NULL,
    status             VARCHAR(30) NOT NULL,
    descricao_problema TEXT        NOT NULL,
    data_abertura      TIMESTAMP   NOT NULL,

    CONSTRAINT pk_ordens_servico PRIMARY KEY (id),
    CONSTRAINT uk_ordens_servico_protocolo UNIQUE (numero_protocolo),

    CONSTRAINT fk_ordens_servico_cliente
        FOREIGN KEY (cliente_id)
            REFERENCES clientes (id),

    CONSTRAINT fk_ordens_servico_equipamento
        FOREIGN KEY (equipamento_id)
            REFERENCES equipamentos (id)
);

CREATE TABLE historico_status
(
    id               BIGSERIAL    NOT NULL,
    ordem_servico_id BIGINT       NOT NULL,
    status_anterior  VARCHAR(30),
    novo_status      VARCHAR(30)  NOT NULL,
    data_alteracao   TIMESTAMP    NOT NULL,
    responsavel      VARCHAR(150) NOT NULL,
    observacao       VARCHAR(500),

    CONSTRAINT pk_historico_status PRIMARY KEY (id),

    CONSTRAINT fk_historico_status_ordem_servico
        FOREIGN KEY (ordem_servico_id)
            REFERENCES ordens_servico (id)
);
