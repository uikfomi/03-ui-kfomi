package com.salgaderia.app.model;

public record Entregador(Long id, String nome, String telefone, String placaVeiculo, boolean ativo) {
    @Override public String toString() {
        return placaVeiculo == null ? nome : nome + " · " + placaVeiculo;
    }
}
