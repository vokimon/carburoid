# Contributing to Carburoid

Thank you for helping improve **Carburoid**!  
We welcome contributions of all kinds: bug reports, code, documentation, and more.

## 📝 Copyright and Licensing

Carburoid is licensed under the **GNU Affero General Public License v3.0 or later**  
([SPDX: AGPL-3.0-or-later](https://spdx.org/licenses/AGPL-3.0-or-later.html)).

By contributing, you agree that:
- Your work is your own or properly attributed.
- You license your contribution under **AGPL-3.0-or-later**.
- You retain copyright to your contributions.
- The project is maintained by **The Carburoid Contributors**, an informal collective.

No copyright assignment is required.

## 🐛 Reporting

- Report in https://github.com/vokimon/carburoid

## 🌿 Branching and Merging

- **One logical feature or fix per branch** (e.g., `feat/nfc-config`, `fix/geocoder-fallback`).
- Keep branches focused and short-lived.
- **Rebase your branch onto `master` before merging** to ensure a clean, linear history.
- Pull requests must be mergeable with **fast-forward**—no merge commits.

## 💬 Commits

- Use **[Gitmoji](https://gitmoji.dev/)** for all commits.
- Prefer granular commits where differences can be easily spotted
- Use separate commits for code 🎨 formatting/style/reordering changes and for logic or behavior changes.  

## ✅ Testing

- All code not requiring Android instrumentation must be developed using Test Driven Development.
- Ensure every assert in your tests fails before implementing the code to make it green.
- Do not consider compilation failures or run-time exceptions as a Red, just failed asserts.
- Android-dependent components
  (e.g., Activities, Services, BroadcastReceivers)
  may be tested via instrumented tests or manual QA when unit testing is impractical.
  Justify exceptions in your pull request.

### 🎨 Code Style

- Code style is enforced with **ktlint**.
- Use descriptive, non abbreviated, context aware identifiers

### 🌐 Translations

- All translations are managed via **Weblate**.
- If you’re a translator, please contribute at: https://hosted.weblate.org/settings/carburoid/carburoid-ui/
- Developers: Add new strings in the default language file `en.yaml` (English).
- `strings.xml` files are generated, do not edit them.
- Developers: only add or update keys in the **default (English) `strings.xml`**; mark non-translatable strings with `translatable="false"`.

### 🔄 Pull Requests

- Link related issues (e.g., `Closes #123`).
- Describe **what changed** and **why**.
- Include screenshots for UI changes.
- Ensure all CI checks pass (build, ktlint, tests).

## 🤝 Code of Conduct

Be respectful, patient, and inclusive.  
Carburoid is built by volunteers—kindness goes a long way.

---

*Carburoid thrives because of its contributors. Thank you for your rigor, care, and commitment to freedom-respecting software.*
