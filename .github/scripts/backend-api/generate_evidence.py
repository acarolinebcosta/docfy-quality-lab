#!/usr/bin/env python3
"""Generate a GitHub summary and safe Allure environment metadata from Surefire XML."""

from __future__ import annotations

import os
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path


MODULE_DIR = Path("tests/backend-api")
REPORTS_DIR = MODULE_DIR / "target/surefire-reports"
ALLURE_RESULTS_DIR = MODULE_DIR / "target/allure-results"
SUMMARY_FILE = MODULE_DIR / "target/backend-api-summary.md"


@dataclass
class TestTotals:
    total: int = 0
    failures: int = 0
    errors: int = 0
    skipped: int = 0

    @property
    def passed(self) -> int:
        return self.total - self.failures - self.errors - self.skipped

    def add(self, suite: ET.Element) -> None:
        self.total += int(suite.attrib.get("tests", 0))
        self.failures += int(suite.attrib.get("failures", 0))
        self.errors += int(suite.attrib.get("errors", 0))
        self.skipped += int(suite.attrib.get("skipped", 0))

    @property
    def passed_gate(self) -> bool:
        return self.passed > 0 and self.failures == 0 and self.errors == 0


def read_surefire_results() -> tuple[TestTotals, TestTotals, TestTotals, TestTotals]:
    totals = TestTotals()
    backend_api = TestTotals()
    json_schema = TestTotals()
    architecture = TestTotals()

    for report in sorted(REPORTS_DIR.glob("TEST-*.xml")):
        suite = ET.parse(report).getroot()
        totals.add(suite)
        suite_name = suite.attrib.get("name", "")
        if suite_name.endswith(".ArchitectureTest"):
            architecture.add(suite)
        else:
            backend_api.add(suite)
        if suite_name.endswith(".PlatformContractTest"):
            json_schema.add(suite)

    return totals, backend_api, json_schema, architecture


def test_gate_status(totals: TestTotals) -> str:
    if totals.total == 0 or totals.skipped == totals.total:
        return "⚪ Not executed"
    if totals.passed_gate:
        return "✅ Passed"
    return "❌ Failed"


def build_gate_status(build_result: str) -> str:
    if build_result == "success":
        return "✅ Passed"
    if build_result in {"failure", "cancelled"}:
        return "⚠️ See build log"
    return "⚪ Not executed"


def property_value(value: str) -> str:
    return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "")


def write_summary(
    totals: TestTotals,
    backend_api: TestTotals,
    json_schema: TestTotals,
    architecture: TestTotals,
    metadata: dict[str, str],
    build_result: str,
) -> None:
    build_status = build_gate_status(build_result)
    result_label = build_result.replace("_", " ").title() if build_result else "Unknown"

    summary = f"""# Docfy Quality Gate

| Execution | Value |
| --- | --- |
| Quality Lab SHA | `{metadata['Quality Lab SHA']}` |
| Docfy SUT SHA | `{metadata['Docfy SUT SHA']}` |
| Environment | {metadata['Environment']} |
| Java version | `{metadata['Java Version']}` |
| Base URL | `{metadata['Base URL']}` |

## Backend/API

| Total | Passed | Failures | Errors | Skipped |
| ---: | ---: | ---: | ---: | ---: |
| {totals.total} | {totals.passed} | {totals.failures} | {totals.errors} | {totals.skipped} |

## Quality Gates

| Gate | Result |
| --- | --- |
| JUnit / REST Assured | {test_gate_status(backend_api)} |
| JSON Schema | {test_gate_status(json_schema)} |
| ArchUnit | {test_gate_status(architecture)} |
| Maven Enforcer | {build_status} |
| Spotless | {build_status} |

## Build result

**{result_label}**
"""

    SUMMARY_FILE.parent.mkdir(parents=True, exist_ok=True)
    SUMMARY_FILE.write_text(summary, encoding="utf-8")

def required_environment(name: str) -> str:
    value = os.environ.get(name, "").strip()
    return value or "Unavailable"


def write_allure_environment(metadata: dict[str, str]) -> None:
    ALLURE_RESULTS_DIR.mkdir(parents=True, exist_ok=True)

    safe_keys = {
        "Quality Lab SHA": "Quality_Lab_SHA",
        "Docfy SUT SHA": "Docfy_SUT_SHA",
        "Environment": "Environment",
        "Java Version": "Java_Version",
        "Base URL": "Base_URL",
    }

    content = "\n".join(
        f"{safe_keys[key]}={property_value(value)}"
        for key, value in metadata.items()
    )

    (ALLURE_RESULTS_DIR / "environment.properties").write_text(
        content + "\n",
        encoding="utf-8",
    )


def main() -> None:
    build_result = os.environ.get("BUILD_RESULT", "unknown").strip().lower()

    metadata = {
        "Quality Lab SHA": required_environment("QUALITY_LAB_SHA"),
        "Docfy SUT SHA": required_environment("DOCFY_SUT_SHA"),
        "Environment": required_environment("CI_ENVIRONMENT"),
        "Java Version": required_environment("JAVA_VERSION"),
        "Base URL": required_environment("DOCFY_API_BASE_URL"),
    }

    totals, backend_api, json_schema, architecture = read_surefire_results()

    write_allure_environment(metadata)

    write_summary(
        totals,
        backend_api,
        json_schema,
        architecture,
        metadata,
        build_result,
    )


if __name__ == "__main__":
    main()
