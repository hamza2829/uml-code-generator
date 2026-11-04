"""Report actual Surefire results and JaCoCo bundle line counters, without dependencies."""
import json
import os
from pathlib import Path
import xml.etree.ElementTree as ET

reports = sorted(Path("target/surefire-reports").glob("TEST-*.xml"))
if not reports:
    raise SystemExit("No Surefire XML reports found")
totals = dict(tests=0, failures=0, errors=0, skipped=0)
for report in reports:
    suite = ET.parse(report).getroot()
    for key in totals:
        totals[key] += int(suite.attrib.get(key, "0"))
root = ET.parse("target/site/jacoco/jacoco.xml").getroot()
line = next(counter for counter in root.findall("counter") if counter.attrib["type"] == "LINE")
covered, missed = int(line.attrib["covered"]), int(line.attrib["missed"])
percent = 100 * covered / (covered + missed)
metrics = {**totals, "line_covered": covered, "line_missed": missed,
           "line_coverage_percent": round(percent, 2), "commit": os.getenv("GITHUB_SHA", "unknown"),
           "run_url": f'https://github.com/{os.getenv("GITHUB_REPOSITORY", "hamza2829/uml-code-generator")}/actions/runs/{os.getenv("GITHUB_RUN_ID", "unknown")}'}
Path("target/ci-metrics.json").write_text(json.dumps(metrics, indent=2) + "\n", encoding="utf-8")
table = ("| Metric | Measured in GitHub Actions |\n| --- | ---: |\n"
         f'| JUnit tests | {totals["tests"]} |\n'
         f'| Failures / errors / skipped | {totals["failures"]} / {totals["errors"]} / {totals["skipped"]} |\n'
         f"| JaCoCo line coverage | {percent:.2f}% ({covered}/{covered + missed} lines) |\n"
         "| Required line coverage | 80% |\n")
Path("target/ci-metrics.md").write_text(table, encoding="utf-8")
print(json.dumps(metrics, indent=2))
if os.getenv("GITHUB_STEP_SUMMARY"):
    with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as summary:
        summary.write(table)
if totals["failures"] or totals["errors"] or totals["skipped"] or percent < 80:
    raise SystemExit("Tests or coverage did not meet the required gate")
