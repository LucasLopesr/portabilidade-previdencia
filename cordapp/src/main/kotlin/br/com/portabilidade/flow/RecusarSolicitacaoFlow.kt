package br.com.portabilidade.flow

import br.com.portabilidade.contract.SolicitacaoContract
import br.com.portabilidade.model.StatusSolicitacao
import br.com.portabilidade.state.FundoPrevidenciaState
import br.com.portabilidade.state.SolicitacaoState
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

object RecusarSolicitacaoFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator(val idSolicitacao: UUID): FlowLogic<SignedTransaction>() {
        override fun call(): SignedTransaction {
            val solicitacaoInput =
                    serviceHub.vaultService.queryBy<SolicitacaoState>(
                            QueryCriteria.LinearStateQueryCriteria(
                                    uuid = listOf(idSolicitacao))
                    ).states.single()

            val notary = solicitacaoInput.state.notary

            val solicitacaoOutput = solicitacaoInput.state.data.copy(
                    statusSolicitacao = StatusSolicitacao.RECUSADO )

            val comando = Command(SolicitacaoContract.Commands.Recusar(),
                    solicitacaoOutput.participants.map { it.owningKey })

            val txBuilder = TransactionBuilder(notary)
                    .addCommand(comando)
                    .addInputState(solicitacaoInput)
                    .addOutputState(solicitacaoOutput,
                            SolicitacaoContract::class.java.canonicalName)

            txBuilder.verify(serviceHub)

            val txAssinadaPorMim = serviceHub.signInitialTransaction(txBuilder)

            val sessao = initiateFlow(solicitacaoOutput.solicitante)
            val txTotalmenteAssinada = subFlow(
                    CollectSignaturesFlow(txAssinadaPorMim, listOf(sessao)))

            return subFlow(FinalityFlow(txTotalmenteAssinada))
        }
    }

    @InitiatedBy(Initiator::class)
    class Acceptor(val otherParty: FlowSession): FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherParty) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                }
            }

            return subFlow(signTransactionFlow)
        }
    }
}