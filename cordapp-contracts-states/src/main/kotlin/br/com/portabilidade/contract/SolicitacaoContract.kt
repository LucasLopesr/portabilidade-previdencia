package br.com.portabilidade.contract

import br.com.portabilidade.model.StatusSolicitacao
import br.com.portabilidade.state.FundoPrevidenciaState
import br.com.portabilidade.state.SolicitacaoState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class SolicitacaoContract: Contract {

    override fun verify(tx: LedgerTransaction) {
        val comando = tx.commandsOfType<Commands>().single()
        when(comando.value) {
            is Commands.Solicitar -> verifySolicitar(tx)
            is Commands.Aceitar -> verifyAceitar(tx)
            is Commands.Recusar -> verifyRecusar(tx)
            else -> throw IllegalArgumentException("Comando desconhecido.")
        }
    }

    fun verifySolicitar(tx: LedgerTransaction) {
        requireThat {
            "Não deve haver inputs." using (
                    tx.inputsOfType<SolicitacaoState>().isEmpty())
            "Deve haver apenas um output." using (
                    tx.outputsOfType<SolicitacaoState>().size == 1 )
            "O Status da solicitação deve ser SOLICITADO." using (
                    tx.outputsOfType<SolicitacaoState>().all {
                        it.statusSolicitacao == StatusSolicitacao.SOLICITADO
                    } )
            "O solicitante e o solicitado devem ser diferentes." using (
                    tx.outputsOfType<SolicitacaoState>().all {
                        it.solicitante != it.solicitado
                    } )
            "O numero de identificação não pode ser vazio." using (
                    tx.outputsOfType<SolicitacaoState>().all {
                        it.documentoIdentificacao.isNotBlank()
                    } )
        }
    }

    inline fun <reified T: LinearState> verificarApenasUmInputEOutput(tx: LedgerTransaction){
        requireThat {
            "Deve haver um input de ${T::class.java.canonicalName}." using (
                    tx.inputsOfType<T>().size == 1)
            "Deve haver um output de ${T::class.java.canonicalName}." using (
                    tx.outputsOfType<T>().size == 1)
        }
    }

    fun verifyAceitar(tx: LedgerTransaction){
        requireThat {
            verificarApenasUmInputEOutput<SolicitacaoState>(tx)
            verificarApenasUmInputEOutput<FundoPrevidenciaState>(tx)

            "O Status da Solicitacao deveria ser SOLICITADO." using (
                    tx.inputsOfType<SolicitacaoState>().all {
                        it.statusSolicitacao == StatusSolicitacao.SOLICITADO
                    } )
            "O Status da Solicitacao deve ser ACEITO." using (
                    tx.outputsOfType<SolicitacaoState>().all {
                        it.statusSolicitacao == StatusSolicitacao.ACEITO
                    } )

            val outputSolicitacao = tx.outputsOfType<SolicitacaoState>().single()
            val outputFundo = tx.outputsOfType<FundoPrevidenciaState>().single()

            "O id do Fundo deve ser igual ao numero de identificação." using (
                    outputSolicitacao.documentoIdentificacao ==
                            outputFundo.fundoPrevidencia.idContrato )

            val inputFundo = tx.inputsOfType<FundoPrevidenciaState>().single()
            val inputSolicitacao = tx.inputsOfType<SolicitacaoState>().single()
            "O dono do Fundo deve ser alterado." using (
                    inputFundo.dono != outputFundo.dono )
            "Apenas o Status da Solicitacao deve ser alterado." using (
                    inputSolicitacao.solicitado == outputSolicitacao.solicitado &&
                            inputSolicitacao.solicitante == outputSolicitacao.solicitante &&
                            inputSolicitacao.documentoIdentificacao == outputSolicitacao.documentoIdentificacao
                    )
            "Apenas o Dono do Fundo deve ser alterado." using (
                    inputFundo.fundoPrevidencia == outputFundo.fundoPrevidencia
                    )
        }
    }

    fun verifyRecusar(tx: LedgerTransaction){
        TODO()
    }

    interface Commands: CommandData {
        class Solicitar: Commands
        class Aceitar: Commands
        class Recusar: Commands
    }

}