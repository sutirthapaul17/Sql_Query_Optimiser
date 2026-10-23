package org.example.queryoptimizer.Service.DocumentationService;


import org.example.queryoptimizer.Dto.Documentation.DocumentationPage;
import org.example.queryoptimizer.Dto.Documentation.DocumentationSource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class DocumentationFetcherService {

    public DocumentationPage fetch(DocumentationSource source)
            throws IOException {

        validateUrl(source.url());

        Document page = Jsoup.connect(source.url())
                .userAgent("SQL-Optimization-Assistant/1.0")
                .timeout(15000)
                .get();

        Element main = page.selectFirst("#docContent");

        if (main == null) {
            main = page.selectFirst("main");
        }

        if (main == null) {
            throw new IOException(
                    "Could not locate main content: " + source.url());
        }

        StringBuilder content = new StringBuilder();

        Elements blocks = main.select(
                "h1, h2, h3, h4, h5, h6, p, pre, ul, ol, table, dl"
        );

        for (Element block : blocks) {

            // Avoid repeating nested paragraphs and lists.
            Element finalMain = main;
            if (block.parents().stream().anyMatch(parent ->
                    parent != finalMain
                            && parent.is(
                            "p, pre, ul, ol, table, dl, " +
                                    "h1, h2, h3, h4, h5, h6"
                    ))) {
                continue;
            }

            String text = block.tagName().equals("pre")
                    ? block.wholeText().trim()
                    : block.text().trim();

            if (text.isBlank()) {
                continue;
            }

            if (block.tagName().matches("h[1-6]")) {
                content.append("\n\n## ")
                        .append(text)
                        .append("\n\n");
            } else {
                content.append(text).append("\n\n");
            }
        }

        if (content.toString().isBlank()) {
            throw new IOException(
                    "No readable content extracted: " + source.url());
        }

        return new DocumentationPage(
                source.title(),
                source.url(),
                source.topic(),
                content.toString().trim()
        );
    }

    private void validateUrl(String url) throws IOException {
        try {
            java.net.URI uri = java.net.URI.create(url);

            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || !(uri.getHost().equalsIgnoreCase("postgresql.org")
                    || uri.getHost().toLowerCase()
                    .endsWith(".postgresql.org"))) {
                throw new IOException(
                        "Only HTTPS PostgreSQL documentation URLs " +
                                "are allowed: " + url);
            }
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid documentation URL", ex);
        }
    }

}
