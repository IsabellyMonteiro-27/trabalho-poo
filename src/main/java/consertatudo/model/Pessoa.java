package consertatudo.model;

import consertatudo.exception.RegraNegocioException;

/**
 * Abstração comum a clientes e técnicos. Concentra a identidade e a validação
 * dos dados básicos (encapsulamento: os campos só mudam por setters que validam).
 */
public abstract class Pessoa {

    private final int id;
    private String nome;
    private String telefone;

    protected Pessoa(int id, String nome, String telefone) {
        this.id = id;
        setNome(nome);
        setTelefone(telefone);
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public final void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("O nome não pode ser vazio.");
        }
        this.nome = nome.trim();
    }

    public final void setTelefone(String telefone) {
        String t = telefone == null ? "" : telefone.trim();
        if (!t.isEmpty() && !t.matches("[0-9 ()+\\-]{8,20}")) {
            throw new RegraNegocioException(
                    "Telefone inválido. Use apenas números, espaços, parênteses, '+' e '-' (mínimo 8 caracteres).");
        }
        this.telefone = t;
    }

    /** Papel da pessoa no sistema (polimorfismo). */
    public abstract String getPapel();

    @Override
    public String toString() {
        return getPapel() + " #" + id + " - " + nome;
    }
}
