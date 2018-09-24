package br.com.portabilidade.flow

import br.com.portabilidade.contract.FundoPrevidenciaContract
import br.com.portabilidade.model.FundoPrevidencia
import br.com.portabilidade.state.FundoPrevidenciaState
import net.corda.core.contracts.Command
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

object CriarFundoPrevidenciaFlow {

    @InitiatingFlow
    @StartableByRPC
    class Initiator(val fundoPrevidencia: FundoPrevidencia): FlowLogic<SignedTransaction>() {

        override fun call(): SignedTransaction {
            val notary = serviceHub.networkMapCache.notaryIdentities.first()

            val output = FundoPrevidenciaState(ourIdentity, fundoPrevidencia)

            val comando = Command(FundoPrevidenciaContract.Commands.Create(), output.dono.owningKey)

            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(output, FundoPrevidenciaContract::class.java.canonicalName)
                    .addCommand(comando)

            txBuilder.verify(serviceHub)

            val txAssinadaPorMim = serviceHub.signInitialTransaction(txBuilder)

            // Não há coleta de assinaturas, apenas o dono do fundo precisa assinar,
            // estamos notorizando para garantir que ninguém vai gastar este fundo mais de uma vez
            return subFlow(FinalityFlow(txAssinadaPorMim))
        }

    }
}