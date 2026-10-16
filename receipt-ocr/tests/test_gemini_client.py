import importlib.util
import json
import pathlib
import sys
import types
import unittest
from unittest.mock import patch


# The unit under test needs only requests.post and RequestException; importing the
# large PaddleOCR image is intentionally unnecessary for these network tests.
requests_stub = types.ModuleType("requests")
requests_stub.RequestException = type("RequestException", (Exception,), {})
requests_stub.post = None
sys.modules.setdefault("requests", requests_stub)
module_path = pathlib.Path(__file__).resolve().parents[1] / "gemini_client.py"
spec = importlib.util.spec_from_file_location("gemini_client", module_path)
client = importlib.util.module_from_spec(spec)
spec.loader.exec_module(client)


class GeminiClientTest(unittest.TestCase):
    def test_success_uses_header_and_bounded_timeout(self):
        response = types.SimpleNamespace(
            status_code=200,
            json=lambda: {"candidates": [{"content": {"parts": [
                {"text": json.dumps({"amount": 15000, "paymentMethod": "현금"})}
            ]}}]},
        )
        with patch.object(client.requests, "post", return_value=response) as post:
            result = client.generate_json("receipt text", "secret-key")
        self.assertEqual(result["amount"], 15000)
        self.assertEqual(post.call_args.kwargs["headers"], {"x-goog-api-key": "secret-key"})
        self.assertEqual(post.call_args.kwargs["timeout"], (3, 12))
        self.assertNotIn("secret-key", post.call_args.args[0])

    def test_transient_failure_retries_once_then_falls_back(self):
        response = types.SimpleNamespace(status_code=503)
        with patch.object(client.requests, "post", return_value=response) as post, \
                patch.object(client.time, "sleep"):
            self.assertIsNone(client.generate_json("receipt", "secret-key"))
        self.assertEqual(post.call_count, 2)

    def test_invalid_model_does_not_make_request(self):
        with patch.object(client.requests, "post") as post:
            self.assertIsNone(client.generate_json("receipt", "secret-key", "../../bad"))
        post.assert_not_called()

    def test_malformed_fields_are_safe_and_unknown_payment_is_not_card(self):
        result = client.normalize_receipt_data({
            "spentOn": "2026-99-99",
            "amount": "15,000",
            "category": ["운영비"],
            "paymentMethod": "동아리 카드",
            "items": [{"name": "음료", "quantity": "2", "price": 1000}],
        })
        self.assertIsNone(result["spentOn"])
        self.assertIsNone(result["amount"])
        self.assertEqual(result["category"], "기타")
        self.assertEqual(result["paymentMethod"], "동아리 카드")
        self.assertEqual(result["items"], [])
        self.assertEqual(client.normalize_receipt_data({})["paymentMethod"], "기타")


if __name__ == "__main__":
    unittest.main()
