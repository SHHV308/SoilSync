document.addEventListener('DOMContentLoaded', async () => {
    const productList = document.getElementById('productList');
    const plantList = document.getElementById('plantList');
    const orderList = document.getElementById('orderList');
    const aiResponse = document.getElementById('aiResponse');
    const chatForm = document.getElementById('chatForm');
    const chatInput = document.getElementById('chatInput');
    const farmRegionInput = document.getElementById('farmRegion');
    const farmCropInput = document.getElementById('farmCrop');
    const growthStageInput = document.getElementById('growthStage');
    const soilTypeInput = document.getElementById('soilType');
    const adviceSubmit = document.getElementById('adviceSubmit');
    const cartItems = document.getElementById('cartItems');
    const cartTotal = document.getElementById('cartTotal');
    const checkoutForm = document.getElementById('checkoutForm');
    const customerNameInput = document.getElementById('customerName');

    let allProducts = [];
    let cart = [];

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
        recommendationsHeading.textContent = 'Field-specific guidance';
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
        aiResponse.textContent = 'Finding your location and checking the live local forecast...';
        adviceSubmit.disabled = true;

        try {
            const response = await fetchJson('/api/ai/advice', {
                method: 'POST',
                body: JSON.stringify({
                    region: farmRegionInput.value.trim(),
                    crop: farmCropInput.value,
                    growthStage: growthStageInput.value,
                    soilType: soilTypeInput.value,
                    question: chatInput.value.trim()
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

    checkoutForm.addEventListener('submit', handleCheckout);
    renderCart();
    await loadData();
});
