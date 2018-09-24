package br.com.portabilidade.model

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class FundoPrevidencia(
        val valor: Int,
        val idContrato: String,
        val beneficiario: String
)