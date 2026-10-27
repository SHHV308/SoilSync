document.addEventListener('DOMContentLoaded', async () => {
    const plantList = document.getElementById('plantList');
    const aiResponse = document.getElementById('aiResponse');
    const chatForm = document.getElementById('chatForm');
    const chatInput = document.getElementById('chatInput');
    const farmRegionInput = document.getElementById('farmRegion');
    const farmCropInput = document.getElementById('farmCrop');
    const locationMode = document.getElementById('locationMode');
    const placeLocationFields = document.getElementById('placeLocationFields');
    const coordinateLocationFields = document.getElementById('coordinateLocationFields');
    const findPlaceButton = document.getElementById('findPlaceButton');
    const findCoordinatesButton = document.getElementById('findCoordinatesButton');
    const locationCandidatesLabel = document.getElementById('locationCandidatesLabel');
    const locationCandidatesSelect = document.getElementById('locationCandidates');
    const latitudeInput = document.getElementById('latitude');
    const longitudeInput = document.getElementById('longitude');
    const coordinateRegionInput = document.getElementById('coordinateRegion');
    const coordinateCountryInput = document.getElementById('coordinateCountry');
    const coordinateCountryCodeInput = document.getElementById('coordinateCountryCode');
    const growthStageInput = document.getElementById('growthStage');
    const soilTypeInput = document.getElementById('soilType');
    const adviceSubmit = document.getElementById('adviceSubmit');

    let locationResults = [];
    let selectedLocation = null;

    const t = (key, params) => i18n.t(key, params);
    let lastPlants = [];
    let shownInitial = '';

    let i18nReady = false;
    try {
        await i18n.init();
        i18nReady = true;
    } catch (error) {
        console.error(error); // The page keeps its built-in English text.
    }
    if (i18nReady) {
        shownInitial = t('advisor.initial');
        aiResponse.textContent = shownInitial;
        i18n.onChange(() => {
            const previous = shownInitial;
            shownInitial = t('advisor.initial');
            if (aiResponse.textContent.trim() === previous) aiResponse.textContent = shownInitial;
            renderPlants(lastPlants);
        });
    }

    async function fetchJson(url, options = {}) {
        const response = await fetch(url, {
            headers: { 'Content-Type': 'application/json' },
            ...options
        });

        if (!response.ok) {
            const errorText = await response.text();
            let message = errorText;
            try {
                const errorBody = JSON.parse(errorText);
                message = errorBody.message || errorBody.error || errorText;
            } catch {
                // Keep a non-JSON response message as-is.
            }
            throw new Error(message || t('err.requestFailed', { status: response.status }));
        }

        return response.json();
    }

    function renderCropAdvice(response) {
        aiResponse.replaceChildren();
        aiResponse.classList.remove('ai-response-error');

        const heading = document.createElement('h3');
        heading.textContent = `${response.crop} · ${response.location}`;
        aiResponse.append(heading);

        if (i18n.language !== 'en') {
            const englishOnly = document.createElement('p');
            englishOnly.className = 'advice-updated';
            englishOnly.textContent = t('advisor.englishOnly');
            aiResponse.append(englishOnly);
        }

        if (response.locationDetails) {
            const metadata = document.createElement('p');
            const userSupplied = response.locationDetails.metadataSource === 'User-provided coordinates and region'
                ? t('res.userSupplied')
                : '';
            metadata.textContent = t('res.meta', response.locationDetails) + userSupplied;
            aiResponse.append(metadata);
        }

        const answer = document.createElement('p');
        answer.textContent = response.answer;
        aiResponse.append(answer);

        const weather = document.createElement('div');
        weather.className = 'weather-summary';
        weather.textContent = t('res.weatherNow', { temp: response.weather.temperatureC.toFixed(1), humidity: response.weather.humidityPercent, precip: response.weather.precipitationMm.toFixed(1) });
        aiResponse.append(weather);

        const forecast = document.createElement('p');
        forecast.textContent = response.weather.forecastSummary;
        aiResponse.append(forecast);

        const recommendationsHeading = document.createElement('h4');
        recommendationsHeading.textContent = t('res.guidance');
        aiResponse.append(recommendationsHeading);

        const recommendations = document.createElement('ul');
        response.recommendations.forEach(item => {
            const listItem = document.createElement('li');
            listItem.textContent = item;
            recommendations.append(listItem);
        });
        aiResponse.append(recommendations);

        const sourcesHeading = document.createElement('h4');
        sourcesHeading.textContent = t('res.limits');
        aiResponse.append(sourcesHeading);

        const sources = document.createElement('ul');
        response.dataSources.forEach(item => {
            const listItem = document.createElement('li');
            listItem.textContent = item;
            sources.append(listItem);
        });
        aiResponse.append(sources);

        const updated = document.createElement('small');
        updated.className = 'advice-updated';
        updated.textContent = t('res.checked', { time: new Date(response.fetchedAt).toLocaleString(i18n.language) });
        aiResponse.append(updated);
    }

    function clearLocationSelection() {
        selectedLocation = null;
        locationResults = [];
        locationCandidatesLabel.hidden = true;
        locationCandidatesSelect.replaceChildren();
    }

    function syncLocationMode() {
        const coordinatesMode = locationMode.value === 'coordinates';
        placeLocationFields.hidden = coordinatesMode;
        coordinateLocationFields.hidden = !coordinatesMode;
        farmRegionInput.required = !coordinatesMode;
        latitudeInput.required = coordinatesMode;
        longitudeInput.required = coordinatesMode;
        coordinateRegionInput.required = coordinatesMode;
        coordinateCountryInput.required = coordinatesMode;
        coordinateCountryCodeInput.required = coordinatesMode;
        clearLocationSelection();
    }

    function displayLocationCandidate(candidate) {
        return [candidate.name, candidate.admin1, candidate.admin2, candidate.country]
            .filter(value => value && value.trim())
            .filter((value, index, values) => values.indexOf(value) === index)
            .join(', ');
    }

    function showLocationCandidates(candidates) {
        locationResults = candidates;
        locationCandidatesSelect.replaceChildren(new Option(t('loc.choose'), ''));
        for (const candidate of candidates) {
            locationCandidatesSelect.add(new Option(
                `${displayLocationCandidate(candidate)} (${candidate.countryCode})`,
                candidate.id
            ));
        }

        if (candidates.length === 1) {
            selectedLocation = candidates[0];
            locationCandidatesLabel.hidden = true;
            aiResponse.textContent = t('loc.selected', { place: displayLocationCandidate(selectedLocation) });
            return true;
        }

        selectedLocation = null;
        locationCandidatesLabel.hidden = candidates.length === 0;
        if (candidates.length > 1) {
            aiResponse.textContent = t('loc.several');
        } else {
            aiResponse.textContent = t('loc.none');
        }
        return false;
    }

    async function searchForLocation() {
        clearLocationSelection();
        let candidates;
        if (locationMode.value === 'coordinates') {
            const inputs = [
                latitudeInput,
                longitudeInput,
                coordinateRegionInput,
                coordinateCountryInput,
                coordinateCountryCodeInput
            ];
            if (inputs.some(input => !input.reportValidity())) return false;

            candidates = await fetchJson('/api/ai/locations', {
                method: 'POST',
                body: JSON.stringify({
                    latitude: Number(latitudeInput.value),
                    longitude: Number(longitudeInput.value),
                    region: coordinateRegionInput.value.trim(),
                    country: coordinateCountryInput.value.trim(),
                    countryCode: coordinateCountryCodeInput.value.trim()
                })
            });
        } else {
            const query = farmRegionInput.value.trim();
            if (query.length < 2) {
                farmRegionInput.focus();
                farmRegionInput.setCustomValidity(t('loc.minChars'));
                farmRegionInput.reportValidity();
                farmRegionInput.setCustomValidity('');
                return false;
            }
            candidates = await fetchJson(`/api/ai/locations?query=${encodeURIComponent(query)}`);
        }
        return showLocationCandidates(candidates);
    }

    function renderPlants(plants) {
        lastPlants = plants;
        if (!plantList) return;
        plantList.innerHTML = plants.map(plant => `
            <article class="plant-card">
                <div class="badge">${plant.healthStatus}</div>
                <h3>${plant.name}</h3>
                <p class="plant-meta">${plant.variety} · ${plant.location}</p>
                <p class="plant-meta">${t('plant.soil', { soil: plant.soilType })}</p>
                <p class="plant-meta">${t('plant.nextWatering', { time: new Date(plant.nextWateringAt).toLocaleString(i18n.language) })}</p>
                <p class="plant-meta">${plant.notes}</p>
            </article>
        `).join('');
    }

    function renderDashboard(dashboard) {
        document.getElementById('heroHealthyPlots').textContent = dashboard.plantCount || 0;
        document.getElementById('heroReminders').textContent = dashboard.activeReminderCount || 0;
    }

    async function loadData() {
        try {
            const [plants, dashboard] = await Promise.all([
                fetchJson('/api/plants'),
                fetchJson('/api/dashboard')
            ]);
            renderPlants(plants);
            renderDashboard(dashboard);
        } catch (error) {
            aiResponse.textContent = t('err.loadData');
            console.error(error);
        }
    }

    chatForm.addEventListener('submit', async (event) => {
        event.preventDefault();

        aiResponse.classList.remove('ai-response-error');
        aiResponse.textContent = t('advisor.checking');
        adviceSubmit.disabled = true;

        try {
            if (!selectedLocation && !await searchForLocation()) return;

            const response = await fetchJson('/api/ai/advice', {
                method: 'POST',
                body: JSON.stringify({
                    region: locationMode.value === 'coordinates'
                        ? coordinateRegionInput.value.trim()
                        : farmRegionInput.value.trim(),
                    crop: farmCropInput.value,
                    growthStage: growthStageInput.value,
                    soilType: soilTypeInput.value,
                    question: chatInput.value.trim(),
                    location: selectedLocation
                })
            });
            renderCropAdvice(response);
        } catch (error) {
            aiResponse.classList.add('ai-response-error');
            aiResponse.textContent = error.message || t('err.advisor');
            console.error(error);
        } finally {
            adviceSubmit.disabled = false;
        }
    });

    findPlaceButton.addEventListener('click', async () => {
        findPlaceButton.disabled = true;
        try {
            await searchForLocation();
        } catch (error) {
            aiResponse.classList.add('ai-response-error');
            aiResponse.textContent = error.message || t('err.locationService');
        } finally {
            findPlaceButton.disabled = false;
        }
    });

    findCoordinatesButton.addEventListener('click', async () => {
        findCoordinatesButton.disabled = true;
        try {
            await searchForLocation();
        } catch (error) {
            aiResponse.classList.add('ai-response-error');
            aiResponse.textContent = error.message || t('err.locationCheck');
        } finally {
            findCoordinatesButton.disabled = false;
        }
    });

    locationMode.addEventListener('change', syncLocationMode);
    farmRegionInput.addEventListener('input', clearLocationSelection);
    for (const input of [
        latitudeInput,
        longitudeInput,
        coordinateRegionInput,
        coordinateCountryInput,
        coordinateCountryCodeInput
    ]) {
        input.addEventListener('input', clearLocationSelection);
    }
    locationCandidatesSelect.addEventListener('change', () => {
        selectedLocation = locationResults.find(candidate => candidate.id === locationCandidatesSelect.value) || null;
        if (selectedLocation) {
            aiResponse.textContent = t('loc.selected', { place: displayLocationCandidate(selectedLocation) });
        }
    });

    syncLocationMode();
    await loadData();
});
