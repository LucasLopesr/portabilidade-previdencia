package br.com.portabilidade.flow

import br.com.portabilidade.model.FundoPrevidencia
import br.com.portabilidade.model.StatusSolicitacao
import br.com.portabilidade.state.FundoPrevidenciaState
import br.com.portabilidade.state.SolicitacaoState
import br.com.portabilidade.utils.criarFundoPrevidencia
import br.com.portabilidade.utils.criarSolicitacao
import com.google.common.collect.testing.Helpers
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class AceitarSolicitacaoFlowTest {

    private val network = MockNetwork(listOf("br.com.portabilidade"))
    private val a = network.createNode()
    private val b = network.createNode()

    init {
        listOf(a, b).forEach {
            it.registerInitiatedFlow(SolicitarFundoPrevidenciaFlow.Acceptor::class.java)
            it.registerInitiatedFlow(AceitarSolicitacaoFlow.Acceptor::class.java)
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


        val flow = AceitarSolicitacaoFlow.Initiator(solicitacao.linearId.id)
        val future = a.startFlow(flow)
        network.runNetwork()

        future.getOrThrow()

        listOf(a, b).forEach {
            it.transaction {
                val vaultState = it.services.vaultService.queryBy<SolicitacaoState>().states.single().state.data
                assertEquals(b.info.legalIdentities.first(), vaultState.solicitante)
                assertEquals(a.info.legalIdentities.first(), vaultState.solicitado)
                assertEquals(StatusSolicitacao.ACEITO, vaultState.statusSolicitacao)
                assertEquals(fundoPrevidencia.idContrato, vaultState.documentoIdentificacao)
            }
        }
        a.transaction {
            Helpers.assertEmpty(a.services.vaultService.queryBy<FundoPrevidenciaState>().states)
        }
        b.transaction {
            val vaultState = b.services.vaultService.queryBy<FundoPrevidenciaState>().states.single().state.data
            assertEquals(fundoPrevidencia, vaultState.fundoPrevidencia)
            assertEquals(b.info.legalIdentities.first(), vaultState.dono)
        }

    }
}