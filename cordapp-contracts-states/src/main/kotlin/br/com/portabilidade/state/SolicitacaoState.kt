package br.com.portabilidade.state

import br.com.portabilidade.model.StatusSolicitacao
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class SolicitacaoState(
        val solicitante: Party,
        val solicitado: Party,
        val documentoIdentificacao: String,
        val statusSolicitacao: StatusSolicitacao = StatusSolicitacao.SOLICITADO,
        override val linearId: UniqueIdentifier = UniqueIdentifier()): LinearState {
    override val participants: List<AbstractParty> =
            listOf(solicitante, solicitado)
}