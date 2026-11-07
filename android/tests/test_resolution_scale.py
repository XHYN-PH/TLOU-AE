"""Exercise generated launch patches; all fixtures are synthetic."""
import struct
import unittest
from test_performance_patches import fixture, entries, ROOT
from unittest.mock import patch, Mock
import tlou_patches as profiles
import tempfile
from pathlib import Path


class ResolutionScaleTests(unittest.TestCase):
    def test_verified_presets_keep_identical_instructions(self):
        for percent, preset in ((33, '360'), (50, '540'), (67, '720')):
            self.assertEqual(profiles.scaled_resolution_patches(percent),
                             profiles.scene_aim_resolution_patches(preset))

    def test_all_endpoints_and_intermediate_scales(self):
        for percent in range(10, 101):
            width, height = profiles.scale_dimensions(percent)
            self.assertEqual(width * 9, height * 16)
            self.assertGreater(width, 0)
            changes = profiles.scaled_resolution_patches(percent)
            data = {at: bytes.fromhex(raw) for at, _, raw in changes}
            self.assertEqual(struct.unpack('<I', data[0x910B22])[0], width)
            self.assertEqual(struct.unpack('<I', data[0x910B29])[0], height)
            self.assertEqual(struct.unpack_from('<f', data[0x9D4023], 1)[0],
                             struct.unpack('<f', struct.pack('<f', 1.0 / height))[0])
            for at in profiles.POSTPROCESS_FULL_SITES + profiles.POSTPROCESS_HALF_SITES:
                target = (width, height) if at in profiles.POSTPROCESS_FULL_SITES else (width//2, height//2)
                self.assertIn(b'\xb9'+struct.pack('<I', target[0])+b'\x41\xb8'+struct.pack('<I', target[1]), data[at])
            for _, original, replacement in changes:
                self.assertEqual(len(original), len(replacement))

    def test_invalid_values_and_wrong_game_are_rejected(self):
        for value in (0, 1, 9, 101, -1, True, 50.0, '50', []):
            with self.assertRaises(ValueError):
                profiles.selected_options({'resolution_scale': value})
        with self.assertRaises(ValueError):
            profiles.selected_options({'resolution_scale': 50}, '01.11')
        self.assertEqual(profiles.selected_options({'resolution_scale': 100}), {})

    def test_atomic_publication_and_unknown_executable(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); fixture(root)
            trusted = patch.object(profiles.hashlib, 'sha256', return_value=Mock(hexdigest=lambda: profiles.BASE_SELF_SHA256))
            with trusted:
                profiles.write_selected(ROOT, root, {'resolution_scale': 50})
            self.assertEqual(entries(root/'patches.bin')[0x910B22], struct.pack('<I', 960))
            before = (root/'patches.bin').read_bytes()
            with self.assertRaises(ValueError):
                profiles.write_selected(ROOT, root, {'resolution_scale': 10})
            self.assertEqual(before, (root/'patches.bin').read_bytes())


if __name__ == '__main__': unittest.main()
