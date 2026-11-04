"""Exercise the packaged CLI in runner subprocesses and verify invalid-model diagnostics."""
from pathlib import Path
import re
import subprocess

jar = "target/uml-code-generator-1.0.0.jar"
models = sorted(Path("models").glob("*.xmi"))
if len(models) < 4:
    raise SystemExit("At least four sample models are required")
expected = {"unique-names": 1, "missing-types": 1, "multiplicities": 1,
            "cyclic-generalization": 2, "unnamed-elements": 2,
            "naming-conventions": 3, "blocks-without-parts": 1}
for model in models:
    validation = subprocess.run(["java", "-jar", jar, "validate", str(model)], text=True, capture_output=True)
    print(validation.stdout, end="")
    if model.stem == "invalid":
        assert validation.returncode == 1, validation
        counts = {}
        for rule in re.findall(r"^(?:ERROR|WARNING) \[([^]]+)\]", validation.stdout, re.MULTILINE):
            counts[rule] = counts.get(rule, 0) + 1
        assert counts == expected, counts
        output = Path("target/generated/invalid")
        generation = subprocess.run(["java", "-jar", jar, "generate", str(model), "--output", str(output)], text=True, capture_output=True)
        assert generation.returncode == 1 and not output.exists(), generation
        assert "\tat " not in generation.stderr, generation.stderr
        continue
    assert validation.returncode == 0, validation.stderr
    for mode in ("original", "flat"):
        output = Path("target/generated") / model.stem / mode
        command = ["java", "-jar", jar, "generate", str(model), "--output", str(output)]
        if mode == "flat":
            command.append("--flatten")
        result = subprocess.run(command, text=True, capture_output=True)
        print(result.stdout, end="")
        assert result.returncode == 0, result.stderr
        sources = sorted((output / "java").rglob("*.java"))
        assert sources and (output / "model.puml").is_file(), output
        classes = output / "classes"
        classes.mkdir()
        subprocess.run(["javac", "--release", "17", "-d", str(classes), *map(str, sources)], check=True)
for path in Path(".").rglob("*"):
    if not path.is_file() or ".git" in path.parts or "target" in path.parts:
        continue
    assert chr(0x2014) not in path.read_text(encoding="utf-8"), f"Forbidden punctuation: {path}"
print("Every valid sample compiled in both modes; the invalid sample failed as expected.")
