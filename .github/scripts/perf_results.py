#!/usr/bin/env python3
"""Performance results per commit (23.11): the Macrobenchmarks of `:benchmark` and the parsing test of 17.6.

  record   append the results of the last local runs to docs/perf-results.csv, tagged with the commit
  check    warn about every series whose latest result is over its budget, and about this run's parsing times

The Macrobenchmarks need a phone, so CI only sees what was recorded; the parsing test runs in CI every time.
Budgets: docs/metricas.md.
"""
import csv
import glob
import json
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from datetime import date

RESULTS = "docs/perf-results.csv"
FIELDS = ["date", "commit", "version", "device", "benchmark", "metric", "value"]
BENCHMARK_OUTPUT = "benchmark/build/outputs/connected_android_test_additional_output/benchmark"
PARSING_REPORTS = "data/build/test-results/*/TEST-*.LargeFeedParsingTest.xml"
PARSING_TIME = re.compile(r"1000-episode feed: (\d+) ms")
PERCENTILES = ("P50", "P90", "P99")
MB = 1024  # memory metrics come in KB

FRAME_BUDGETS = {"frameOverrunMs.P90": 0, "frameOverrunMs.P99": 16}
BUDGETS = {
    "StartupBenchmark.coldStartWithoutCompilation": {"timeToInitialDisplayMs.median": 1000},
    "StartupBenchmark.coldStartPartiallyCompiled": {"timeToInitialDisplayMs.median": 1000},
    "UsageBenchmark.scrollTheEpisodes": {
        **FRAME_BUDGETS,
        "frameDurationCpuMs.P99": 24,
        "memoryHeapSizeMaxKb.median": 100 * MB,
        "memoryRssAnonMaxKb.median": 220 * MB,
    },
    "UsageBenchmark.openThePlayer": FRAME_BUDGETS,
    "LargeFeedParsingTest": {"ms": 2000},
}


def benchmark_rows(path):
    data = json.load(open(path))
    device = data["context"]["build"]["model"]
    for bench in data["benchmarks"]:
        name = f"{bench['className'].rsplit('.', 1)[-1]}.{bench['name']}"
        for metric, values in bench.get("metrics", {}).items():
            yield device, name, f"{metric}.median", values["median"]
        for metric, values in bench.get("sampledMetrics", {}).items():
            for p in PERCENTILES:
                yield device, name, f"{metric}.{p}", values[p]


def parsing_rows():
    for path in glob.glob(PARSING_REPORTS):
        target = path.split(os.sep)[-2]  # desktopTest, iosSimulatorArm64Test, …
        found = PARSING_TIME.search(ET.parse(path).findtext("system-out") or "")
        if found:
            yield target, "LargeFeedParsingTest", "ms", int(found.group(1))


def over_budget(benchmark, metric, value):
    budget = BUDGETS.get(benchmark, {}).get(metric)
    return budget if budget is not None and float(value) > budget else None


def record():
    git = lambda *args: subprocess.run(["git", *args], capture_output=True, text=True, check=True).stdout.strip()
    commit = git("rev-parse", "--short", "HEAD") + ("-dirty" if git("status", "--porcelain", "--untracked-files=no") else "")
    version = re.search(r"APP_VERSION=(.+)", open("version.properties").read()).group(1).strip()
    found = [r for p in glob.glob(f"{BENCHMARK_OUTPUT}/**/*-benchmarkData.json", recursive=True) for r in benchmark_rows(p)]
    found += list(parsing_rows())
    with open(RESULTS, "a", newline="") as out:
        writer = csv.writer(out)
        for device, benchmark, metric, value in found:
            writer.writerow([date.today().isoformat(), commit, version, device, benchmark, metric, round(value, 1)])
    print(f"{len(found)} results recorded for {commit}")


def check():
    latest = {}
    with open(RESULTS) as rows:
        for row in csv.DictReader(rows):
            latest[(row["device"], row["benchmark"], row["metric"])] = row
    runs = [{"device": d, "benchmark": b, "metric": m, "value": v, "commit": "this run"} for d, b, m, v in parsing_rows()]
    for row in list(latest.values()) + runs:
        budget = over_budget(row["benchmark"], row["metric"], row["value"])
        if budget is not None:
            print(f"::warning::{row['benchmark']} {row['metric']} on {row['device']} is {row['value']}, "
                  f"over its budget of {budget} (commit {row['commit']})")
    for row in runs:
        print(f"{row['benchmark']} on {row['device']}: {row['value']} ms")


if __name__ == "__main__":
    {"record": record, "check": check}[sys.argv[1]]()
