package br.com.portabilidade.flow

import br.com.portabilidade.model.FundoPrevidencia
import br.com.portabilidade.model.StatusSolicitacao
import br.com.portabilidade.state.SolicitacaoState
import br.com.portabilidade.utils.criarFundoPrevidencia
import br.com.portabilidade.utils.criarSolicitacao
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class RecusarSolicitacaoFlowTest {

    private val network = MockNetwork(listOf("br.com.portabilidade"))
    private val a = network.createNode()
    private val b = network.createNode()

    init {
        listOf(a, b).forEach {
            it.registerInitiatedFlow(SolicitarFundoPrevidenciaFlow.Acceptor::class.java)
            it.registerInitiatedFlow(RecusarSolicitacaoFlow.Acceptor::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `deve salvar a solicitacao no vault dos dois nos`() {

        val fundoPrevidencia = FundoPrevidencia(5000, "contrato", "pessoa")

        criarFundoPrevidencia(network, a, fundoPrevidencia)
        val solicitacao = criarSolicitacao(network, b, a.info.legalIdentities.first(), fundoPrevidencia.idContrato)

        val flow = RecusarSolicitacaoFlow.Initiator(solicitacao.linearId.id)
        val future = a.startFlow(flow)
        network.runNetwork()

        future.getOrThrow()

        listOf(a, b).forEach {
            it.transaction {
                val vaultState = it.services.vaultService.queryBy<SolicitacaoState>().states.single().state.data
                assertEquals(b.info.legalIdentities.first(), vaultState.solicitante)
                assertEquals(a.info.legalIdentities.first(), vaultState.solicitado)
                assertEquals(StatusSolicitacao.RECUSADO, vaultState.statusSolicitacao)
                assertEquals(fundoPrevidencia.idContrato, vaultState.documentoIdentificacao)
            }
        }

    }
}