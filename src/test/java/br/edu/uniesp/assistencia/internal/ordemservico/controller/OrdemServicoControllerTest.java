package br.edu.uniesp.assistencia.internal.ordemservico.controller;

import br.edu.uniesp.assistencia.internal.cliente.entity.ClienteEntity;
import br.edu.uniesp.assistencia.internal.cliente.repository.ClienteRepository;
import br.edu.uniesp.assistencia.internal.equipamento.entity.EquipamentoEntity;
import br.edu.uniesp.assistencia.internal.equipamento.repository.EquipamentoRepository;
import br.edu.uniesp.assistencia.internal.ordemservico.entity.HistoricoStatusEntity;
import br.edu.uniesp.assistencia.internal.ordemservico.entity.OrdemServicoEntity;
import br.edu.uniesp.assistencia.internal.ordemservico.entity.StatusOrdemServico;
import br.edu.uniesp.assistencia.internal.ordemservico.repository.OrdemServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrdemServicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private EquipamentoRepository equipamentoRepository;

    @Autowired
    private OrdemServicoRepository ordemServicoRepository;

    @BeforeEach
    void limparBanco() {
        ordemServicoRepository.deleteAll();
        equipamentoRepository.deleteAll();
        clienteRepository.deleteAll();
    }

    private EquipamentoEntity equipamentoDe(ClienteEntity cliente) {
        EquipamentoEntity equipamento = new EquipamentoEntity("Notebook", "Dell", "Inspiron 15", "SERIE-123");
        equipamento.associarCliente(cliente);
        return equipamentoRepository.save(equipamento);
    }

    @Test
    void cenario1_sucessoNaAbertura() throws Exception {
        ClienteEntity cliente = clienteRepository.save(new ClienteEntity("Maria Silva", "11122233344", "maria@email.com"));
        EquipamentoEntity equipamento = equipamentoDe(cliente);

        mockMvc.perform(post("/ordens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(cliente.getId(), equipamento.getId(), "Notebook não liga após queda de energia.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.numeroProtocolo").value(containsString("OS-")))
                .andExpect(jsonPath("$.status").value("RECEBIDA"))
                .andExpect(jsonPath("$.dataAbertura").isNotEmpty());
    }

    @Test
    void cenario2_camposObrigatoriosInvalidos() throws Exception {
        mockMvc.perform(post("/ordens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\": null, \"equipamentoId\": null, \"descricaoProblema\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.clienteId").isNotEmpty())
                .andExpect(jsonPath("$.erros.equipamentoId").isNotEmpty())
                .andExpect(jsonPath("$.erros.descricaoProblema").isNotEmpty());
    }

    @Test
    void cenario3_clienteOuEquipamentoInexistente() throws Exception {
        mockMvc.perform(post("/ordens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(999999L, 999999L, "Descrição qualquer")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.recurso").value("Cliente"));
    }

    @Test
    void cenario4_clienteInativo() throws Exception {
        ClienteEntity cliente = new ClienteEntity("João Inativo", "55566677788", "joao.inativo@email.com");
        cliente.inativar();
        cliente = clienteRepository.save(cliente);
        EquipamentoEntity equipamento = equipamentoDe(cliente);

        mockMvc.perform(post("/ordens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(cliente.getId(), equipamento.getId(), "Notebook não liga.")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Não é permitido abrir Ordem de Serviço para um cliente inativo."));
    }

    @Test
    void cenario5_equipamentoDeOutroCliente() throws Exception {
        ClienteEntity cliente = clienteRepository.save(new ClienteEntity("Carlos Souza", "99988877766", "carlos@email.com"));
        ClienteEntity outroCliente = clienteRepository.save(new ClienteEntity("Ana Lima", "12312312312", "ana@email.com"));
        EquipamentoEntity equipamentoDeOutro = equipamentoDe(outroCliente);

        mockMvc.perform(post("/ordens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(cliente.getId(), equipamentoDeOutro.getId(), "Notebook não liga.")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("O equipamento informado não pertence ao cliente indicado."));
    }

    @Test
    @Transactional
    void cenario6_registraProtocoloEHistoricoDeAbertura() throws Exception {
        ClienteEntity cliente = clienteRepository.save(
                new ClienteEntity("Pedro Teste", "77788899900", "pedro@email.com"));
        EquipamentoEntity equipamento = equipamentoDe(cliente);

        MvcResult result = mockMvc.perform(post("/ordens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(cliente.getId(), equipamento.getId(), "Equipamento não liga.")))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        OrdemServicoEntity os = ordemServicoRepository.findById(id).orElseThrow();

        assertThat(os.getNumeroProtocolo()).matches("^OS-\\d{8}-[A-F0-9]{4}$");
        assertThat(os.getStatus()).isEqualTo(StatusOrdemServico.RECEBIDA);
        assertThat(os.getDataAbertura()).isNotNull();
        assertThat(os.getHistorico()).hasSize(1);

        HistoricoStatusEntity historico = os.getHistorico().get(0);
        assertThat(historico.getStatusAnterior()).isNull();
        assertThat(historico.getNovoStatus()).isEqualTo(StatusOrdemServico.RECEBIDA);
        assertThat(historico.getResponsavel()).isEqualTo("SISTEMA");
        assertThat(historico.getObservacao()).contains("Abertura");
    }

    private String payload(Long clienteId, Long equipamentoId, String descricao) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "clienteId", clienteId,
                "equipamentoId", equipamentoId,
                "descricaoProblema", descricao
        ));
    }
}
