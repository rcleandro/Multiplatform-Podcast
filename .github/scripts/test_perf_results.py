"""python3 -m unittest discover -s .github/scripts"""
import contextlib
import io
import os
import tempfile
import unittest

import perf_results

HEADER = "date,commit,version,device,benchmark,metric,value\n"
STARTUP = "StartupBenchmark.coldStartWithoutCompilation,timeToInitialDisplayMs.median"


class CheckTest(unittest.TestCase):
    def check(self, *rows):
        with tempfile.NamedTemporaryFile("w", suffix=".csv", delete=False) as results:
            results.write(HEADER + "".join(f"{row}\n" for row in rows))
        perf_results.RESULTS, perf_results.PARSING_REPORTS = results.name, "/nonexistent/*.xml"
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            perf_results.check()
        os.remove(results.name)
        return out.getvalue()

    def test_warns_when_the_latest_result_is_over_budget(self):
        out = self.check(f"2026-10-23,aaa,1.0.0,razr,{STARTUP},548", f"2026-10-24,bbb,1.0.0,razr,{STARTUP},1200")
        self.assertIn("::warning::", out)
        self.assertIn("bbb", out)

    def test_an_old_result_over_budget_no_longer_warns(self):
        out = self.check(f"2026-10-23,aaa,1.0.0,razr,{STARTUP},1200", f"2026-10-24,bbb,1.0.0,razr,{STARTUP},548")
        self.assertNotIn("::warning::", out)

    def test_negative_frame_overrun_is_within_budget(self):
        out = self.check("2026-10-24,aaa,1.0.0,razr,UsageBenchmark.openThePlayer,frameOverrunMs.P90,-5.9")
        self.assertNotIn("::warning::", out)


if __name__ == "__main__":
    unittest.main()
