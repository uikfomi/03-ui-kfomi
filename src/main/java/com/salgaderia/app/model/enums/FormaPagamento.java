package com.salgaderia.app.model.enums;

public enum FormaPagamento {
    DINHEIRO {
        @Override
        public boolean isPermiteTroco() {
            return true;
        }
    },
    PIX {
        @Override
        public boolean isPermiteTroco() {
            return false;
        }
    },
    CARTAO {
        @Override
        public boolean isPermiteTroco() {
            return false;
        }
    },
    CARTAO_CREDITO {
        @Override
        public boolean isPermiteTroco() {
            return false;
        }
    },
    CARTAO_APP {
        @Override public boolean isPermiteTroco() { return false; }
    },
    CARTAO_DEBITO {
        @Override
        public boolean isPermiteTroco() {
            return false;
        }
    };

    public abstract boolean isPermiteTroco();

    public String descricao() {
        return switch (this) {
            case PIX -> "PIX";
            case DINHEIRO -> "Dinheiro";
            case CARTAO -> "Cartão";
            case CARTAO_APP -> "Cartão App";
            case CARTAO_CREDITO -> "Cartão de crédito";
            case CARTAO_DEBITO -> "Cartão de débito";
        };
    }
}
