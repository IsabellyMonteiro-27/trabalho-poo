package consertatudo.persistence;

import consertatudo.exception.EntidadeNaoEncontradaException;
import consertatudo.exception.PersistenciaException;
import consertatudo.util.Csv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Repositório genérico que guarda os objetos em memória e os persiste em um CSV.
 * As subclasses só dizem como converter um objeto de/para uma linha.
 *
 * Tratamento de erros: arquivo ausente vira repositório vazio e linhas
 * corrompidas são ignoradas com um aviso, sem derrubar o sistema.
 */
public abstract class RepositorioCsv<T> {

    private final Path arquivo;
    private final List<T> itens = new ArrayList<>();
    private int linhasIgnoradas;

    protected RepositorioCsv(Path arquivo) {
        this.arquivo = arquivo;
    }

    protected abstract String cabecalho();

    protected abstract T deCampos(List<String> campos);

    protected abstract List<String> paraCampos(T item);

    protected abstract int idDe(T item);

    protected abstract String nomeEntidade();

    public void carregar() {
        itens.clear();
        linhasIgnoradas = 0;
        if (!Files.exists(arquivo)) {
            return; // primeira execução: começa vazio
        }
        try {
            String conteudo = new String(Files.readAllBytes(arquivo), StandardCharsets.UTF_8);
            String[] linhas = conteudo.split("\\r?\\n");
            for (int i = 1; i < linhas.length; i++) { // linha 0 é o cabeçalho
                if (linhas[i].isBlank()) {
                    continue;
                }
                try {
                    itens.add(deCampos(Csv.separar(linhas[i])));
                } catch (RuntimeException e) {
                    linhasIgnoradas++;
                    System.err.println("Aviso: linha " + (i + 1) + " de " + arquivo.getFileName()
                            + " ignorada (dado inválido).");
                }
            }
        } catch (IOException e) {
            throw new PersistenciaException("Não foi possível ler " + arquivo.getFileName(), e);
        }
    }

    public void salvar() {
        try {
            Path pasta = arquivo.toAbsolutePath().getParent();
            if (pasta != null) {
                Files.createDirectories(pasta);
            }
            List<String> linhas = new ArrayList<>();
            linhas.add(cabecalho());
            for (T item : itens) {
                linhas.add(Csv.juntar(paraCampos(item)));
            }
            Path temporario = arquivo.resolveSibling(arquivo.getFileName() + ".tmp");
            Files.write(temporario, linhas, StandardCharsets.UTF_8);
            try {
                Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new PersistenciaException("Não foi possível gravar " + arquivo.getFileName(), e);
        }
    }

    public void adicionar(T item) {
        itens.add(item);
    }

    public void remover(T item) {
        itens.remove(item);
    }

    public List<T> listar() {
        return Collections.unmodifiableList(itens);
    }

    public Optional<T> buscarPorId(int id) {
        return itens.stream().filter(i -> idDe(i) == id).findFirst();
    }

    public T exigirPorId(int id) {
        return buscarPorId(id).orElseThrow(() -> new EntidadeNaoEncontradaException(nomeEntidade(), id));
    }

    public int proximoId() {
        return itens.stream().mapToInt(this::idDe).max().orElse(0) + 1;
    }

    public int getLinhasIgnoradas() {
        return linhasIgnoradas;
    }
}
