package br.edu.uniesp.assistencia.internal.ordemservico.entity;

import br.edu.uniesp.assistencia.internal.cliente.entity.ClienteEntity;
import br.edu.uniesp.assistencia.internal.equipamento.entity.EquipamentoEntity;
import br.edu.uniesp.assistencia.shared.exception.RegraNegocioException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "ordens_servico")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrdemServicoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_protocolo", nullable = false, unique = true, length = 30)
    private String numeroProtocolo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClienteEntity cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipamento_id", nullable = false)
    private EquipamentoEntity equipamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOrdemServico status = StatusOrdemServico.RECEBIDA;

    @Column(name = "descricao_problema", nullable = false, columnDefinition = "TEXT")
    private String descricaoProblema;

    @Column(name = "data_abertura", nullable = false, updatable = false)
    private LocalDateTime dataAbertura;

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HistoricoStatusEntity> historico = new ArrayList<>();

    public OrdemServicoEntity(String numeroProtocolo, ClienteEntity cliente, EquipamentoEntity equipamento,
                              String descricaoProblema) {
        if (!cliente.isAtivo()) {
            throw new RegraNegocioException("Não é permitido abrir Ordem de Serviço para um cliente inativo.");
        }
        if (!equipamento.getCliente().getId().equals(cliente.getId())) {
            throw new RegraNegocioException("O equipamento informado não pertence ao cliente indicado.");
        }
        this.numeroProtocolo = numeroProtocolo;
        this.cliente = cliente;
        this.equipamento = equipamento;
        this.descricaoProblema = descricaoProblema;
        this.status = StatusOrdemServico.RECEBIDA;
        this.dataAbertura = LocalDateTime.now();
        adicionarHistorico(null, StatusOrdemServico.RECEBIDA, "SISTEMA", "Abertura da ordem de serviço.");
    }

    private void adicionarHistorico(StatusOrdemServico anterior, StatusOrdemServico novo,
                                    String responsavel, String observacao) {
        HistoricoStatusEntity entrada = new HistoricoStatusEntity(this, anterior, novo, responsavel, observacao);
        this.historico.add(entrada);
    }
}
