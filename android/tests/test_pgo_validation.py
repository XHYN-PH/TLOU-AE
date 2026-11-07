import hashlib
import importlib.util
from pathlib import Path
import tempfile
import unittest

spec=importlib.util.spec_from_file_location('tlou_pgo',Path(__file__).parents[1]/'bionic/pgo.py')
pgo=importlib.util.module_from_spec(spec);spec.loader.exec_module(pgo)


class PgoValidationTests(unittest.TestCase):
    def test_profile_must_match_architecture_compiler_sources_and_bytes(self):
        with tempfile.TemporaryDirectory() as temp:
            profile=Path(temp)/'training.profdata';profile.write_bytes(b'synthetic profile identity')
            data={'target':'aarch64-linux-android','compiler':'NDK compiler','native_sources':'source identity',
                  'profile_sha256':hashlib.sha256(profile.read_bytes()).hexdigest()}
            pgo.validate(profile,data,'NDK compiler','source identity')
            for key,value in [('target','x86_64-windows'),('compiler','old compiler'),('native_sources','old source'),('profile_sha256','bad digest')]:
                with self.assertRaises(ValueError):pgo.validate(profile,{**data,key:value},'NDK compiler','source identity')
