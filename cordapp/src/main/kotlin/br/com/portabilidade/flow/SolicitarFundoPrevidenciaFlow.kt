package br.com.portabilidade.flow

import br.com.portabilidade.contract.SolicitacaoContract
import br.com.portabilidade.state.SolicitacaoState
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

object SolicitarFundoPrevidenciaFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator(val idContrato: String,
                    val solicitado: Party): FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            //seleciona o notary
            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            //cria a transacao
                // cria output
                val output = SolicitacaoState(ourIdentity,
                        solicitado,
                        idContrato)
                // cria comando
                val comando = Command(SolicitacaoContract.Commands.Solicitar(),
                        output.participants.map { it.owningKey })

            val txBuilder = TransactionBuilder(notary)
                    .addCommand(comando)
                    .addOutputState(output,
                            SolicitacaoContract::class.java.canonicalName)
            // verifica a transacao

            txBuilder.verify(serviceHub)
            // assinar a transacao

            val txAssinadaPorMim = serviceHub.signInitialTransaction(txBuilder)

            // coletar as assinaturas

            val sessao = initiateFlow(solicitado)

            val txTotalmenteAssinada = subFlow(
                    CollectSignaturesFlow(
                            txAssinadaPorMim,
                            listOf(sessao)))

            // enviar para o notary e gravar na base

            return subFlow(FinalityFlow(txTotalmenteAssinada))
        }
    }

    @InitiatedBy(Initiator::class)
    class Acceptor(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                }
            }

            return subFlow(signTransactionFlow)
        }
    }

}