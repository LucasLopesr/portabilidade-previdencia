package br.com.portabilidade.model

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class StatusSolicitacao {
    SOLICITADO,
    ACEITO,
    RECUSADO
}