package br.com.portabilidade.contract

import br.com.portabilidade.state.FundoPrevidenciaState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class FundoPrevidenciaContract: Contract {

    override fun verify(tx: LedgerTransaction) {
        val comando = tx.commandsOfType<Commands>().single()

        when (comando.value) {
            is Commands.Create -> verifyCreate(tx)
            is Commands.Portar -> verifyPortar(tx)
            else -> throw IllegalArgumentException("Comando ${comando.value::class.java.canonicalName} não reconhecido.")
        }

        requireThat {
            "Todos os participantes devem assinar a transação." using
                    (tx.inputsOfType<FundoPrevidenciaState>() + tx.outputsOfType())
                            .flatMap {
                                it.participants.map {
                                    it.owningKey }
                            }.containsAll(comando.signers)
        }

    }

    fun verifyCreate(tx: LedgerTransaction) {
        val fundosPrevidencia = tx.outputsOfType<FundoPrevidenciaState>().map { it.fundoPrevidencia }
        requireThat {
            "Não deve haver inputs." using ( tx.inputsOfType<FundoPrevidenciaState>().isEmpty() )
            "Deve haver apenas um output." using ( fundosPrevidencia.size == 1 )
            "O valor do Fundo de Previdência deve ser positivo." using ( fundosPrevidencia.all { it.valor > 0 } )
            "O Fundo de Previdência precisa ter um contrato." using ( fundosPrevidencia.all { it.idContrato.isNotBlank() } )
            "O Fundo de Previdência precisa ter um beneficiario." using ( fundosPrevidencia.all { it.beneficiario.isNotBlank() } )
        }
    }

    fun verifyPortar(tx: LedgerTransaction) {
        requireThat {
            val inputFundo = tx.inputsOfType<FundoPrevidenciaState>().single()
            val outputFundo = tx.outputsOfType<FundoPrevidenciaState>().single()

            "O dono do Fundo deve ser alterado." using (
                    inputFundo.dono != outputFundo.dono)
            "Apenas o Dono do Fundo deve ser alterado." using (
                    inputFundo.fundoPrevidencia == outputFundo.fundoPrevidencia
                    )
        }
    }


    interface Commands: CommandData {
        class Create: Commands
        class Portar: Commands
    }

}