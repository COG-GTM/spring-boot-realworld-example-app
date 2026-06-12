---
name: export-deepwiki-documentation-to-confluence
description: "Converted from Devin playbook: Export DeepWiki Documentation to Confluence"
triggers:
  - user
  - model
---

# Export DeepWiki Documentation to Confluence

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Export DeepWiki Documentation to Confluence (playbook-bbe7f9154c094417a0f00bfc672e8bd6)

## Procedure

# Export DeepWiki Documentation to Confluence

## Overview

Export a GitHub repository's DeepWiki documentation to Confluence, preserving the hierarchical structure and formatting. This playbook uses the DeepWiki MCP to fetch documentation and the Atlassian MCP to create Confluence pages that mirror the DeepWiki layout.

## What's Needed From User

- **GitHub repository**: The repository name in `owner/repo` format (e.g., `COG-GTM/Aplicacion-de-Banca-Spring-Boot`)
- **Confluence space key**: The target Confluence space key where pages will be created (e.g., `DOCS`)
- **Atlassian site URL**: The Confluence site URL (e.g., `https://yoursite.atlassian.net`)
- **Parent page ID (optional)**: If the documentation should be nested under an existing page

## Procedure

1. **Verify DeepWiki availability**: Use the DeepWiki MCP `read_wiki_structure` tool with the repository name
   - If it returns "Repository not found", tell the user to visit `https://deepwiki.com/{owner}/{repo}` to trigger indexing and wait for confirmation before proceeding

2. **Verify Confluence access**: Use the Atlassian MCP `getAccessibleAtlassianResources` tool to get the cloudId, then use `getConfluenceSpaces` with the space key to get the numerical spaceId
   - If access fails, inform the user to verify their Atlassian MCP has Confluence scopes configured

3. **Fetch DeepWiki structure**: Call `read_wiki_structure` to get the hierarchical topic list
   - The structure uses numbered sections (e.g., "1 Overview", "1.1 Repository Structure", "2 Build System")
   - Save this structure to determine parent-child relationships

4. **Fetch DeepWiki content**: Call `read_wiki_contents` to get the full markdown documentation
   - The content is returned as a single markdown document with sections delimited by headers

5. **Parse content into sections**: Split the markdown content by matching headers to the structure
   - DeepWiki uses markdown headers (`#`, `##`, `###`) that correspond to the numbered structure
   - For each item in the structure (e.g., "1.1 Repository Structure"), find the matching header in the content
   - Extract the content from that header until the next header of equal or higher level
   - Store each section's content mapped to its structure item

6. **Create the root Confluence page**: Use `createConfluencePage` with:
   - `cloudId`: from step 2
   - `spaceId`: numerical ID from step 2
   - `parentId`: user-provided parent page ID (if any)
   - `title`: repository name or user-specified title (e.g., "BankApp Documentation")
   - `body`: a table of contents listing all sections with brief descriptions
   - `contentFormat`: "markdown"
   - Save the returned page ID for use as parent

7. **Create pages for each top-level section**: For each top-level section (1, 2, 3, etc.):
   - Use `createConfluencePage` with `parentId` set to the root page ID
   - Set `title` to the section name without the number prefix
   - Set `body` to the parsed section content
   - Save the returned page ID for any subsections

8. **Create pages for subsections**: For each subsection (1.1, 1.2, 2.1, etc.):
   - Use `createConfluencePage` with `parentId` set to the parent section's page ID
   - Continue recursively for deeper nesting (1.1.1, etc.)

9. **Verify the page hierarchy**: Use `getConfluencePageDescendants` on the root page to confirm all pages were created with correct hierarchy

10. **Report completion**: Provide the user with the root page URL and a summary of pages created

## Specifications

- Use `contentFormat: "markdown"` for all pages to preserve DeepWiki formatting
- Strip section numbers from page titles (e.g., "1.1 Repository Structure" becomes "Repository Structure")
- The root page should include a header note: "Documentation exported from DeepWiki on {date}"
- Mermaid diagrams in DeepWiki content may not render in Confluence - note this limitation to the user if diagrams are present

## Advice and Pointers

- DeepWiki content uses markdown headers that align with the numbered structure - use regex like `/^#{1,3}\s+(.+)$/gm` to find section boundaries
- The structure numbering indicates hierarchy: single digit (1, 2) = top-level, decimal (1.1, 2.3) = child, multiple decimals (1.1.1) = grandchild
- Confluence has a 255-character page title limit - truncate long titles if needed
- If the DeepWiki content contains mermaid diagrams (```mermaid blocks), inform the user these may need manual conversion to Confluence diagrams or images

## Forbidden Actions

- Do not create pages without first verifying both DeepWiki and Confluence access
- Do not modify existing Confluence pages unless explicitly requested
- Do not include section numbers in Confluence page titles
- Do not proceed if DeepWiki returns "Repository not found"

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
