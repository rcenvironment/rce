import unittest
from unittest import TestCase

from check_repository import *


class Test(TestCase):
    def test_process_project(self):
        script_dir = os.path.dirname(__file__)
        test_cases_dir = os.path.join(script_dir, 'validation_examples')

        for element_name in os.listdir(test_cases_dir):
            element_full_path = os.path.join(test_cases_dir, element_name)
            if os.path.isdir(element_full_path):
                self.__process_validation_example(element_name, element_full_path)

    def __process_validation_example(self, project_name, project_path):

        print('Running scanner on example project "%s"' % project_name)

        # read the expected scanner output from "expected_result.txt" in the project directory
        with open(os.path.join(project_path, 'expected_result.txt'), 'r') as f:
            expected_output = f.read().strip()

        output_collector = OutputCollector(include_project_name_in_output=False)
        ProjectRulesValidator(project_name, project_path, output_collector).apply(enable_clean_repo_checks=True)
        actual_output = output_collector.get_validation_summary().strip()
        if expected_output != actual_output:
            self.fail("Validation of test project \"%s\" failed; Expected output:"
                      "\n---\n%s\n---\nActual output:\n---\n%s\n---" % (
                          project_name, expected_output, actual_output))


if __name__ == '__main__':
    unittest.main()
