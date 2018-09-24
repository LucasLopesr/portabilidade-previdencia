package br.com.portabilidade.flow

import br.com.portabilidade.contract.SolicitacaoContract
import br.com.portabilidade.model.StatusSolicitacao
import br.com.portabilidade.state.FundoPrevidenciaState
import br.com.portabilidade.state.SolicitacaoState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

object AceitarFundoPrevidenciaFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator(val idSolicitacao: UUID): FlowLogic<SignedTransaction>() {
        override fun call(): SignedTransaction {
            // Buscar a solicitacao
            val solicitacaoInput =
                    serviceHub.vaultService.queryBy<SolicitacaoState>(
                            QueryCriteria.LinearStateQueryCriteria(
                                    uuid = listOf(idSolicitacao))
                    ).states.single()

            // Buscar o fundo
            val fundoInput = serviceHub.vaultService.queryBy<FundoPrevidenciaState>(
                    QueryCriteria.LinearStateQueryCriteria(
                            externalId = listOf(solicitacaoInput
                                    .state.data.documentoIdentificacao))
            ).states.single()
            // Selecionar o notary
            val notary = fundoInput.state.notary

            // Construir transacao
                // definir inputs
                // definir outputs
            val fundoOutput = fundoInput.state.data.copy(
                    dono = solicitacaoInput.state.data.solicitante)
            val solicitacaoOutput = solicitacaoInput.state.data.copy(
                    statusSolicitacao = StatusSolicitacao.ACEITO )
                // construir o comando
            val comando = Command(SolicitacaoContract.Commands.Aceitar(),
                    solicitacaoOutput.participants.map { it.owningKey })

            val txBuilder = TransactionBuilder(notary)
                    .addCommand(comando)
                    .addInputState(solicitacaoInput)
                    .addInputState(fundoInput)
                    .addOutputState(solicitacaoOutput,
                            SolicitacaoContract::class.java.canonicalName)
                    .addOutputState(fundoOutput,
                            SolicitacaoContract::class.java.canonicalName)
            // Verifica a transacao
            txBuilder.verify(serviceHub)

            val txAssinadaPorMim = serviceHub.signInitialTransaction(txBuilder)
            // Coleta as assinaturas

            val sessao = initiateFlow(solicitacaoOutput.solicitante)
            val txTotalmenteAssinada = subFlow(
                    CollectSignaturesFlow(txAssinadaPorMim, listOf(sessao)))
            // Finaliza a transacao
            return subFlow(FinalityFlow(txTotalmenteAssinada))
        }
    }

    @InitiatedBy(Initiator::class)
    class Acceptor(val otherParty: FlowSession): FlowLogic<SignedTransaction>() {

        override fun call(): SignedTransaction {
            
        }
    }
}