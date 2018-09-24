package br.com.portabilidade.flow

import br.com.portabilidade.model.FundoPrevidencia
import br.com.portabilidade.state.FundoPrevidenciaState
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class CriarFundoPrevidenciaFlowTest {

    private val network = MockNetwork(listOf("br.com.portabilidade"))
    private val a = network.createNode()

    init {
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `deve criar fundo e armazenar no vault`() {

        val fundoPrevidencia = FundoPrevidencia(5000, "contrato", "pessoa")

        val flow = CriarFundoPrevidenciaFlow.Initiator(fundoPrevidencia)
        val future = a.startFlow(flow)
        network.runNetwork()
        future.getOrThrow()

        a.transaction {
            val vaultState = a.services.vaultService.queryBy<FundoPrevidenciaState>().states.single().state.data
            assertEquals(fundoPrevidencia, vaultState.fundoPrevidencia)
            assertEquals(a.info.legalIdentities.first(), vaultState.dono)
        }
    }
}