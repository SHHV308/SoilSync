document.addEventListener('DOMContentLoaded', async () => {
    const productList = document.getElementById('productList');
    const plantList = document.getElementById('plantList');
    const orderList = document.getElementById('orderList');
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
    const cartItems = document.getElementById('cartItems');
    const cartTotal = document.getElementById('cartTotal');
    const checkoutForm = document.getElementById('checkoutForm');
    const customerNameInput = document.getElementById('customerName');

    let allProducts = [];
    let cart = [];
    let locationResults = [];
    let selectedLocation = null;

    const formatCurrency = (value) => {
        const amount = Number(value || 0);
        return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amount);
    };

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
            throw new Error(message || `Request failed: ${response.status}`);
        }

        return response.json();
    }

    function renderCropAdvice(response) {
        aiResponse.replaceChildren();
        aiResponse.classList.remove('ai-response-error');

        const heading = document.createElement('h3');
        heading.textContent = `${response.crop} · ${response.location}`;
        aiResponse.append(heading);

        if (response.locationDetails) {
            const metadata = document.createElement('p');
            const userSupplied = response.locationDetails.metadataSource === 'User-provided coordinates and region'
                ? ' · Country and region supplied by you'
                : '';
            metadata.textContent = `Country code: ${response.locationDetails.countryCode} · Time zone: ${response.locationDetails.timezone} · Köppen-Geiger climate zone: ${response.locationDetails.climateZone}${userSupplied}`;
            aiResponse.append(metadata);
        }

        const answer = document.createElement('p');
        answer.textContent = response.answer;
        aiResponse.append(answer);

        const weather = document.createElement('div');
        weather.className = 'weather-summary';
        weather.textContent = `Live now: ${response.weather.temperatureC.toFixed(1)}°C · ${response.weather.humidityPercent}% humidity · ${response.weather.precipitationMm.toFixed(1)} mm precipitation`;
        aiResponse.append(weather);

        const forecast = document.createElement('p');
        forecast.textContent = response.weather.forecastSummary;
        aiResponse.append(forecast);

        const recommendationsHeading = document.createElement('h4');
        recommendationsHeading.textContent = 'Guidance and safety';
        aiResponse.append(recommendationsHeading);

        const recommendations = document.createElement('ul');
        response.recommendations.forEach(item => {
            const listItem = document.createElement('li');
            listItem.textContent = item;
            recommendations.append(listItem);
        });
        aiResponse.append(recommendations);

        const sourcesHeading = document.createElement('h4');
        sourcesHeading.textContent = 'Data and limits';
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
        updated.textContent = `Weather checked ${new Date(response.fetchedAt).toLocaleString()}`;
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
        locationCandidatesSelect.replaceChildren(new Option('Choose a matching place...', ''));
        for (const candidate of candidates) {
            locationCandidatesSelect.add(new Option(
                `${displayLocationCandidate(candidate)} (${candidate.countryCode})`,
                candidate.id
            ));
        }

        if (candidates.length === 1) {
            selectedLocation = candidates[0];
            locationCandidatesLabel.hidden = true;
            aiResponse.textContent = `Selected location: ${displayLocationCandidate(selectedLocation)}.`;
            return true;
        }

        selectedLocation = null;
        locationCandidatesLabel.hidden = candidates.length === 0;
        if (candidates.length > 1) {
            aiResponse.textContent = 'Several places match. Choose the correct country and region from the list.';
        } else {
            aiResponse.textContent = 'No matching places were found. Try adding a country or region.';
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
                farmRegionInput.setCustomValidity('Enter at least two characters to search for a place.');
                farmRegionInput.reportValidity();
                farmRegionInput.setCustomValidity('');
                return false;
            }
            candidates = await fetchJson(`/api/ai/locations?query=${encodeURIComponent(query)}`);
        }
        return showLocationCandidates(candidates);
    }

    function addToCart(productId) {
        const existing = cart.find(item => item.productId === productId);
        if (existing) {
            existing.quantity += 1;
        } else {
            cart.push({ productId, quantity: 1 });
        }
        renderCart();
    }

    function changeQuantity(productId, delta) {
        const item = cart.find(entry => entry.productId === productId);
        if (!item) return;
        item.quantity += delta;
        if (item.quantity <= 0) {
            cart = cart.filter(entry => entry.productId !== productId);
        }
        renderCart();
    }

    function getCartItemTotal(product) {
        const item = cart.find(entry => entry.productId === product.id);
        return item ? product.price * item.quantity : 0;
    }

    function renderCart() {
        if (!cartItems) return;

        if (cart.length === 0) {
            cartItems.innerHTML = '<p class="product-meta">Your cart is empty.</p>';
            cartTotal.textContent = formatCurrency(0);
            return;
        }

        const cartSummary = cart.map(entry => {
            const product = allProducts.find(item => item.id === entry.productId);
            if (!product) return '';

            return `
                <div class="cart-item">
                    <div>
                        <strong>${product.name}</strong>
                        <small>${formatCurrency(product.price)} each</small>
                    </div>
                    <div>
                        <div class="quantity-controls">
                            <button type="button" class="qty-button" data-action="decrease" data-product-id="${product.id}">−</button>
                            <span>${entry.quantity}</span>
                            <button type="button" class="qty-button" data-action="increase" data-product-id="${product.id}">+</button>
                        </div>
                    </div>
                </div>
            `;
        }).join('');

        cartItems.innerHTML = cartSummary;
        const total = cart.reduce((sum, entry) => {
            const product = allProducts.find(item => item.id === entry.productId);
            return sum + (product ? product.price * entry.quantity : 0);
        }, 0);
        cartTotal.textContent = formatCurrency(total);

        cartItems.querySelectorAll('.qty-button').forEach(button => {
            button.addEventListener('click', () => {
                const productId = Number(button.dataset.productId);
                const action = button.dataset.action;
                changeQuantity(productId, action === 'increase' ? 1 : -1);
            });
        });
    }

    function renderProducts(products) {
        allProducts = products;
        if (!productList) return;
        productList.innerHTML = products.map(product => `
            <article class="product-card">
                <div class="badge">${product.category}</div>
                <h3>${product.name}</h3>
                <p class="product-meta">${product.description}</p>
                <div class="price">${formatCurrency(product.price)}</div>
                <p class="product-meta">Recommended use: ${product.recommendedUse}</p>
                <p class="product-meta">Stock: ${product.stock} ${product.unit}</p>
                <div class="quantity-row">
                    <span class="product-meta">Add to cart</span>
                    <button type="button" class="primary-button" data-product-id="${product.id}">Add</button>
                </div>
            </article>
        `).join('');

        productList.querySelectorAll('[data-product-id]').forEach(button => {
            button.addEventListener('click', () => addToCart(Number(button.dataset.productId)));
        });
    }

    function renderPlants(plants) {
        if (!plantList) return;
        plantList.innerHTML = plants.map(plant => `
            <article class="plant-card">
                <div class="badge">${plant.healthStatus}</div>
                <h3>${plant.name}</h3>
                <p class="plant-meta">${plant.variety} · ${plant.location}</p>
                <p class="plant-meta">Soil: ${plant.soilType}</p>
                <p class="plant-meta">Next watering: ${new Date(plant.nextWateringAt).toLocaleString()}</p>
                <p class="plant-meta">${plant.notes}</p>
            </article>
        `).join('');
    }

    function renderOrders(orders) {
        if (!orderList) return;
        orderList.innerHTML = orders.map(order => `
            <article class="order-item">
                <div>
                    <h3>${order.customerName}</h3>
                    <p class="product-meta">${new Date(order.createdAt).toLocaleDateString()} · ${order.status}</p>
                </div>
                <strong>${formatCurrency(order.totalAmount)}</strong>
            </article>
        `).join('');
    }

    function renderDashboard(dashboard) {
        document.getElementById('heroHealthyPlots').textContent = dashboard.plantCount || 0;
        document.getElementById('heroReminders').textContent = dashboard.activeReminderCount || 0;
        document.getElementById('heroRevenue').textContent = formatCurrency(dashboard.monthlyRevenue || 0);
        document.getElementById('productCount').textContent = dashboard.productCount || 0;
        document.getElementById('plantCount').textContent = dashboard.plantCount || 0;
        document.getElementById('reminderCount').textContent = dashboard.activeReminderCount || 0;
        document.getElementById('revenueCount').textContent = formatCurrency(dashboard.monthlyRevenue || 0);
        renderOrders(dashboard.recentOrders || []);
    }

    async function loadData() {
        try {
            const [products, plants, dashboard] = await Promise.all([
                fetchJson('/api/products'),
                fetchJson('/api/plants'),
                fetchJson('/api/dashboard')
            ]);
            renderProducts(products);
            renderPlants(plants);
            renderDashboard(dashboard);
            renderCart();
        } catch (error) {
            aiResponse.textContent = 'The app is loading demo data. Refresh after the backend is running.';
            console.error(error);
        }
    }

    async function handleCheckout(event) {
        event.preventDefault();

        if (!cart.length) {
            aiResponse.textContent = 'Add at least one product before placing an order.';
            return;
        }

        const customerName = customerNameInput.value.trim();
        if (!customerName) {
            aiResponse.textContent = 'Please enter a customer name before placing the order.';
            return;
        }

        try {
            const order = await fetchJson('/api/orders', {
                method: 'POST',
                body: JSON.stringify({
                    customerName,
                    items: cart.map(item => ({ productId: item.productId, quantity: item.quantity }))
                })
            });

            aiResponse.innerHTML = `
                <strong>Order placed successfully.</strong><br />
                Customer: ${order.customerName}<br />
                Total: ${formatCurrency(order.totalAmount)}<br />
                Status: ${order.status}
            `;
            cart = [];
            customerNameInput.value = '';
            renderCart();
            await loadData();
        } catch (error) {
            aiResponse.textContent = error.message || 'The order could not be placed right now.';
        }
    }

    chatForm.addEventListener('submit', async (event) => {
        event.preventDefault();

        aiResponse.classList.remove('ai-response-error');
        aiResponse.textContent = 'Checking the selected location and live forecast...';
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
            aiResponse.textContent = error.message || 'The regional advisor could not retrieve live data right now.';
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
            aiResponse.textContent = error.message || 'The location service is unavailable right now.';
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
            aiResponse.textContent = error.message || 'The location could not be checked right now.';
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
            aiResponse.textContent = `Selected location: ${displayLocationCandidate(selectedLocation)}.`;
        }
    });

    checkoutForm.addEventListener('submit', handleCheckout);
    syncLocationMode();
    renderCart();
    await loadData();
});
