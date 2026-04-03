 let foods = JSON.parse(localStorage.getItem("foods")) || [];

function save() {
  localStorage.setItem("foods", JSON.stringify(foods));
}

function goTo(page) {
  window.location.href = page;
}

function addFood() {
  const name = document.getElementById('foodName').value;
  const desc = document.getElementById('description').value;
  const price = document.getElementById('price').value;
  const location = document.getElementById('location').value;
if (!name || !price || !location) {
    alert("Fill all required fields");
    return;
  }

  foods.push({
    name,
    desc,
    price,
    location,
    time: new Date().toLocaleString()
  });

  save();
  displayFoods();
}

function displayFoods(list = foods) {
  const container = document.getElementById("foodList");
  if (!container) return;

  container.innerHTML = "";
 list.forEach((food, index) => {
    container.innerHTML += `
      <div class="card">
        <h3>${food.name}</h3>
        <p>${food.desc || ""}</p>
        <p class="price">₹${food.price}</p>
        <p>📍 ${food.location}</p>
        <small>${food.time}</small>
        <br><br>
        <button onclick="reserve(${index})">Reserve</button>
      </div>
    `;
  });
}
function reserve(index) {
  alert("Food Reserved!");
  foods.splice(index, 1);
  save();
  displayFoods();
}

function searchFood() {
  const q = document.getElementById("search").value.toLowerCase();
  const filtered = foods.filter(f =>
    f.name.toLowerCase().includes(q) ||
    f.location.toLowerCase().includes(q)
  );
  displayFoods(filtered);
}

// auto load
if (document.getElementById("foodList")) {
  displayFoods();
}
