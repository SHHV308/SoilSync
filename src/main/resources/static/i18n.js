// Lightweight i18n. To add a language: add static/i18n/<code>.json (same keys as en.json) and an entry in static/i18n/languages.json.
window.i18n = (() => {
    const STORAGE_KEY = 'soilsync.lang';
    const FALLBACK = 'en';
    const listeners = [];
    let languages = [];
    let fallbackStrings = {};
    let strings = {};
    let meta = {};
    let current = FALLBACK;

    const readStored = () => {
        try { return localStorage.getItem(STORAGE_KEY); } catch { return null; }
    };
    const writeStored = (code) => {
        try { localStorage.setItem(STORAGE_KEY, code); } catch { /* storage may be blocked; the choice just won't persist */ }
    };

    async function loadStrings(code) {
        const response = await fetch(`/i18n/${code}.json`);
        if (!response.ok) throw new Error(`Missing language file: ${code}`);
        return response.json();
    }

    function pickInitial() {
        const supported = languages.map(language => language.code);
        const stored = readStored();
        if (stored && supported.includes(stored)) return stored;
        for (const tag of navigator.languages || [navigator.language]) {
            const base = String(tag || '').toLowerCase().split('-')[0];
            if (supported.includes(base)) return base;
        }
        return FALLBACK;
    }

    function t(key, params = {}) {
        const template = strings[key] ?? fallbackStrings[key] ?? key;
        return template.replace(/\{(\w+)\}/g, (match, name) => (name in params ? params[name] : match));
    }

    function applyToDocument() {
        document.documentElement.lang = current;
        document.documentElement.dir = meta.dir || 'ltr';
        document.title = t('page.title');
        document.querySelectorAll('[data-i18n]').forEach(element => {
            element.textContent = t(element.dataset.i18n);
        });
        document.querySelectorAll('[data-i18n-placeholder]').forEach(element => {
            element.placeholder = t(element.dataset.i18nPlaceholder);
        });
        const notice = document.getElementById('translationNotice');
        if (notice) notice.hidden = !meta.machineTranslated;
    }

    async function setLanguage(code) {
        const loaded = code === FALLBACK ? fallbackStrings : await loadStrings(code);
        strings = loaded;
        meta = loaded._meta || {};
        current = code;
        writeStored(code);
        applyToDocument();
        const select = document.getElementById('languageSelect');
        if (select) select.value = code;
        listeners.forEach(listener => listener(code));
    }

    async function init() {
        const response = await fetch('/i18n/languages.json');
        if (!response.ok) throw new Error('Could not load the language list.');
        languages = await response.json();
        fallbackStrings = await loadStrings(FALLBACK);

        const select = document.getElementById('languageSelect');
        if (select) {
            select.replaceChildren(...languages.map(language => new Option(language.name, language.code)));
            select.addEventListener('change', () => {
                setLanguage(select.value).catch(error => console.error(error));
            });
        }
        await setLanguage(pickInitial());
    }

    return {
        t,
        init,
        onChange: (listener) => listeners.push(listener),
        get language() { return current; }
    };
})();
