# CS4031 Assignment 03 - Bottom-Up Parser Design & Implementation

## Team
- Umar Javed: 22i-1050
- Sheharyar Ahmed: 22i-1171

## Language
Java

## Project Structure
- Project folder structure:

```text
Assignment 03/
├── src/
│   ├── Main.java
│   ├── Grammar.java
│   ├── SLRParser.java
│   ├── LR1Parser.java
│   ├── Items.java
│   ├── ParsingTable.java
│   ├── Stack.java
│   └── Tree.java
├── input/
│   ├── grammar1.txt
│   ├── grammar2.txt
│   ├── grammar3.txt
│   ├── grammar_with_conflict.txt
│   ├── grammar1_inputs.txt
│   ├── grammar2_inputs.txt
│   ├── grammar3_inputs.txt
│   ├── grammar4.txt
│   ├── grammar4_inputs.txt
│   ├── input_invalid.txt
│   └── input_valid.txt
├── output/
│   ├── augmented_grammar.txt
│   ├── slr_items.txt
│   ├── slr_parsing_table.txt
│   ├── slr_trace.txt
│   ├── lr1_items.txt
│   ├── lr1_parsing_table.txt
│   ├── lr1_trace.txt
│   ├── comparison.txt
│   └── parse_trees.txt
├── docs/
│   ├── report.tex
│   └── report.pdf
├── build.bat
└── README.md
```

- `src/` contains the parser implementation.
- `input/` contains sample grammars and input strings.
- `output/` receives generated reports, tables, traces, and parse trees.
- `docs/` is reserved for the written report.

## Build
From the project root, run:

```bat
build.bat
```

Or compile manually:

```bat
javac src\*.java
```

## Run
Run both SLR(1) and LR(1) parsers on one grammar file and one input file:

```bat
java -cp src Main input\grammar2.txt input\input_valid.txt output
```

The input file should contain one test string per line. Blank lines and lines starting with `#` are ignored.

## Grammar File Format
Each production is written on one line:

```text
NonTerminal -> production1 | production2 | production3
```

Rules:
- Use `->` for the production arrow.
- Use `|` for alternatives.
- Separate symbols with spaces.
- Use multi-character non-terminals that start with an uppercase letter.
- Use `epsilon` or `@` for empty productions.

Example:

```text
Expr -> Expr + Term | Term
Term -> Term * Factor | Factor
Factor -> ( Expr ) | id
```

## Input Format
One input string per line. Tokens must also be space-separated.

Examples:
- `id + id * id`
- `( id + id ) * id`
- `if id then other else other`

## Execution Notes
The program generates these files in `output/`:
- `augmented_grammar.txt`
- `slr_items.txt`
- `slr_parsing_table.txt`
- `slr_trace.txt`
- `lr1_items.txt`
- `lr1_parsing_table.txt`
- `lr1_trace.txt`
- `comparison.txt`
- `parse_trees.txt`

## Report Outline
Use this outline for `docs/report.pdf`:

1. Introduction
2. Approach
3. Challenges
4. Test Cases
5. Comparison Analysis
6. Sample Outputs
7. Conclusion

Recommended content for each section:
- Introduction: bottom-up parsing, LR parsing, and assignment goals.
- Approach: data structures, closure/goto, table construction, parse tree handling.
- Challenges: epsilon handling, conflict detection, lookahead propagation.
- Test Cases: at least 3 grammars, 5 inputs per grammar, valid and invalid examples.
- Comparison Analysis: states, table size, build time, parsing speed, conflicts.
- Sample Outputs: item sets, parsing tables, traces, trees, and conflict examples.
- Conclusion: what worked, what was difficult, and what LR(1) adds over SLR(1).

## Sample Commands
SLR and LR(1) are both run by the same executable; the difference is in the generated reports:

```bat
java -cp src Main input\grammar3.txt input\input_valid.txt output
java -cp src Main input\grammar_with_conflict.txt input\input_invalid.txt output
```

## Suggested Test Sets
- `grammar1.txt` with `grammar1_inputs.txt` for simple expression parsing.
- `grammar2.txt` with `grammar2_inputs.txt` for precedence and nesting.
- `grammar3.txt` with `grammar3_inputs.txt` for the classic SLR conflict example.
- `grammar_with_conflict.txt` for a second conflict-heavy grammar.

## Known Limitations
- Tokens must be separated by spaces.
- The parser is designed for assignment-scale grammars and does not include lexical analysis.
- If a grammar has conflicts, the first detected action is preserved and the conflict is reported.
