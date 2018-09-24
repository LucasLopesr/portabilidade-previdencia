package br.com.portabilidade.state

import br.com.portabilidade.model.FundoPrevidencia
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

data class FundoPrevidenciaState(
        val dono: Party,
        val fundoPrevidencia: FundoPrevidencia,
        override val linearId: UniqueIdentifier = UniqueIdentifier(
                externalId = fundoPrevidencia.idContrato)): LinearState {
    override val participants: List<AbstractParty> =
            listOf(dono)
}
