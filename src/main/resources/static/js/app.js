document.querySelectorAll("[data-generating]").forEach((form) => {
    form.addEventListener("submit", () => {
        const button = form.querySelector('button[type="submit"]');
        button.disabled = true;
        button.textContent = "Gerando suas questões…";
        form.querySelector("[data-status]").textContent =
            "Aguarde a resposta da IA. Evite atualizar a página.";
    });
});

const extractButton = document.getElementById("extract-pdf");
if (extractButton) {
    const fileInput = document.getElementById("pdf-file");
    const content = document.getElementById("content");
    const status = document.getElementById("pdf-status");
    const start = document.getElementById("pdf-start");
    const end = document.getElementById("pdf-end");
    const generate = content.form.querySelector('button[type="submit"]');
    const count = document.getElementById("content-count");
    const updateCount = () => (count.textContent = content.value.length);
    content.addEventListener("input", updateCount);
    updateCount();
    const report = (message, error = false) => {
        status.textContent = message;
        status.className = "field-hint " + (error ? "error-text" : "success-text");
    };
    extractButton.addEventListener("click", async () => {
        const file = fileInput.files[0];
        if (!file) {
            report("Selecione um PDF primeiro.", true);
            return;
        }
        if (file.size > 10 * 1024 * 1024) {
            report("O PDF deve ter no máximo 10 MB.", true);
            return;
        }
        if (!start.checkValidity() || !end.checkValidity()) {
            report("Informe páginas inteiras a partir de 1.", true);
            return;
        }
        if (start.value && end.value && Number(end.value) < Number(start.value)) {
            report("A página final deve ser maior ou igual à inicial.", true);
            return;
        }
        if (
            content.value.trim() &&
            !window.confirm(
                "Substituir o conteúdo de estudo pelo texto do PDF? O texto atual será mantido se a extração falhar.",
            )
        )
            return;
        const data = new FormData();
        data.append("file", file);
        if (start.value) data.append("startPage", start.value);
        if (end.value) data.append("endPage", end.value);
        extractButton.disabled = true;
        generate.disabled = true;
        content.readOnly = true;
        fileInput.disabled = true;
        start.disabled = true;
        end.disabled = true;
        report("Extraindo texto… Aguarde.");
        try {
            const response = await fetch(extractButton.dataset.url, { method: "POST", body: data });
            const result = await response.json().catch(() => ({}));
            if (!response.ok)
                throw new Error(
                    result.message ||
                        (response.status === 413
                            ? "O PDF deve ter no máximo 10 MB."
                            : "Não foi possível importar o PDF. Tente novamente."),
                );
            if (typeof result.text !== "string" || result.text.length > 16000)
                throw new Error("O texto não pôde ser importado. Selecione menos páginas.");
            content.value = result.text;
            updateCount();
            let message = `Texto importado: páginas ${result.startPage} a ${result.endPage} de ${result.totalPages}. Revise o conteúdo antes de gerar.`;
            if (result.emptyPages?.length)
                message +=
                    " Páginas sem texto extraível: " +
                    result.emptyPages.join(", ") +
                    ". Elas podem conter imagens ou estar em branco.";
            if (result.text.length < 80)
                message +=
                    " O texto tem menos de 80 caracteres: acrescente conteúdo antes de gerar.";
            report(message);
            content.focus();
        } catch (error) {
            report(error.message || "Falha de conexão ao importar o PDF.", true);
        } finally {
            extractButton.disabled = false;
            generate.disabled = false;
            content.readOnly = false;
            fileInput.disabled = false;
            start.disabled = false;
            end.disabled = false;
        }
    });
}
