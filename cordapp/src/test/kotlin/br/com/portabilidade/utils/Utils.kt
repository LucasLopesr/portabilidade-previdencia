package br.com.portabilidade.utils

import br.com.portabilidade.flow.CriarFundoPrevidenciaFlow
import br.com.portabilidade.flow.SolicitarFundoPrevidenciaFlow
import br.com.portabilidade.model.FundoPrevidencia
import br.com.portabilidade.state.FundoPrevidenciaState
import br.com.portabilidade.state.SolicitacaoState
import net.corda.core.identity.Party
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode

fun criarFundoPrevidencia(network: MockNetwork, node: StartedMockNode, fundoPrevidencia: FundoPrevidencia): FundoPrevidenciaState {
    val flow = CriarFundoPrevidenciaFlow.Initiator(fundoPrevidencia)
    val future = node.startFlow(flow)
    network.runNetwork()
    return future.getOrThrow().coreTransaction.outputsOfType<FundoPrevidenciaState>().single()
}

fun criarSolicitacao(network: MockNetwork, solicitante: StartedMockNode, solicitado: Party, contrato: String): SolicitacaoState {
    val flow = SolicitarFundoPrevidenciaFlow.Initiator(contrato, solicitado)
    val future = solicitante.startFlow(flow)
    network.runNetwork()

    return future.getOrThrow().coreTransaction.outputsOfType<SolicitacaoState>().single()
}