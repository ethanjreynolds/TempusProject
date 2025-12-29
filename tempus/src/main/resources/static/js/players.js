const form_container = document.getElementById("player-form-container");

get_countries();

document.getElementById("player-form").addEventListener("submit", event => {
    event.preventDefault();

    const form_data = new FormData(event.target);

    let params = {};
    form_data.forEach((value, key) => { if (value !== "") params[key] = value });

    fetch('/api/player/create', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded'},
        body: new URLSearchParams(params)
    })
        .then(async res => {
            const div = document.getElementById("player-form-response");
            const body = await res.text();
            if (res.ok) {
                div.innerHTML = `OK: Created player <span style="font-style: italic">${params["name"]}</span> with player_id <a href="/players/${body}">${body}</a>`;
            } else {
                div.textContent = body;
            }
            div.style.padding = "4px";
            div.style.backgroundColor = res.ok ? "green" : "red";
            form_container.style.outline = `${res.ok ? 'green' : 'red'} solid 2px`;
        })
        .catch(error => console.error(error));
});

async function load_more_players(event, button) {
    const type = button.dataset.type;

    const limit = 100;
    let offset = parseInt(button.dataset.offset);

    let res = await fetch(`/api/leaderboard?type=${type}&limit=${limit}&offset=${offset}`);
    if (!res.ok) return;

    let rows = await res.json();

    const tbody = document.getElementById("players-table");

    rows.forEach(p => {
        tbody.insertAdjacentHTML("beforeend", `
            <tr>
                <td class="num">${p.rank}</td>
                <td class="num">${p.points.toFixed(2)}</td>
                <td><a href="/players/${p.player_id}">[${p.rank_title}] ${p.name}</a></td>
            </tr>
        `);
    });

    button.dataset.offset = offset + limit;
}

async function get_countries() {
    let res = await fetch("/api/countries");
    if (res.ok) {
        let select = document.getElementById("player-country");
        (await res.json()).forEach(row => select.insertAdjacentHTML("beforeend", `<option value="${row.code.code}"${row.code.code === 'US' ? ' selected' : ''}>${row.name}</option>`));
    }
}