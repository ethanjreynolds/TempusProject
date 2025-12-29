const form_container = document.getElementById("player-run-form-container");
const form = document.getElementById("player-run-form");
const map_id = document.getElementById('player-run-map-id');
const zone_type = document.getElementById('player-run-zone-type');
const zone_index = document.getElementById('player-run-zone-index');

const player_id = document.getElementById("player-id").dataset.playerId;
const table = document.getElementById("player-runs");
const limit_counter = document.getElementById("limit");

let current_button = "points";
let limit = 100;
let offset = 0;

let btns = [
    {column: "points", direction: -1},
    {column: "tier", direction: -1},
    {column: "rating", direction: 1},
    {column: "rank", direction: 1},
    {column: "time_submitted", direction: 1},
    {column: "duration", direction: -1},
];

let zone_counts = {};
let current_map = zone_counts[0];

update_buttons();
get_zone_counts();
get_servers();

form.addEventListener("submit", event => {
    event.preventDefault();

    const form_data = new FormData(event.target);

    let params = {"player_id": player_id};
    form_data.forEach((value, key) => { if (value !== "") params[key] = value });

    fetch('/api/run/create', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded'},
        body: new URLSearchParams(params)
    })
        .then(async res => {
            const div = document.getElementById("player-run-form-response");
            const data = await res.json();
            div.textContent = data.message;
            div.style.padding = "4px";
            div.style.backgroundColor = res.ok ? "green" : "red";
            form_container.style.outline = `${res.ok ? 'green' : 'red'} solid 2px`;

            if (res.ok) {
                // insert and updates should stay at the top
                table.insertAdjacentHTML("afterbegin", make_row(data.run))
                // updated player stats
                document.getElementById("otitle").innerText = data.stats.overall_title;
                document.getElementById("orank").innerText = data.stats.overall_rank;
                document.getElementById("opts").innerText = data.stats.overall_points.toFixed(2);
                document.getElementById("stitle").innerText = data.stats.soldier_title;
                document.getElementById("srank").innerText = data.stats.soldier_rank;
                document.getElementById("spts").innerText = data.stats.soldier_points.toFixed(2);
                document.getElementById("dtitle").innerText = data.stats.demoman_title;
                document.getElementById("drank").innerText = data.stats.demoman_rank;
                document.getElementById("dpts").innerText = data.stats.demoman_points.toFixed(2);
            }
            
        })
        .catch(error => console.error(error));
});

map_id.addEventListener('change', (event) => {
        current_map = zone_counts.find(z => z.map.id.toString() === event.target.value);

        zone_type.innerHTML = "<option value='map'>Map</option>";
        zone_index.innerHTML = "<option value='1'>1</option>";

        if (current_map.courses > 0) zone_type.insertAdjacentHTML("beforeend", `<option value="course">Course</option>`);
        if (current_map.bonuses > 0) zone_type.insertAdjacentHTML("beforeend", `<option value="bonus">Bonus</option>`);
    }
);

zone_type.addEventListener('change', (event) => {
        zone_index.innerHTML = "";

        let i = 1;
        if (event.target.value === "course") i = current_map.courses;
        else if (event.target.value === "bonus") i = current_map.bonuses;

        for (let j = 1; j <= i; j++) {
            zone_index.insertAdjacentHTML("beforeend", `<option value='${j}'>${j}</option>`);
        }
    }
);

document.getElementById("limit-inc").addEventListener("click", async () => {
    // no point querying rows we already have
    offset = limit;
    limit += 100;
    limit_counter.innerText = limit;
    await update_table(true);
});
document.getElementById("limit-dec").addEventListener("click", () => {
    if (limit >= 200) {
        limit -= 100;
        limit_counter.innerText = limit;
    }
});

btns.forEach((b) => document.getElementById(b.column).addEventListener("click", async () => {
    if (current_button === b.column) b.direction *= -1;
    current_button = b.column;

    // move to beginning
    btns.unshift(btns.splice(btns.findIndex(e => e.column === current_button), 1)[0]);
    update_buttons();

    await update_table();
}));

document.addEventListener("click", function(event) {
    const btn = event.target.closest(".delete-run-btn");
    if (!btn) return;
    if (!confirm("Delete this run permanently?")) return;
    let params = {
        player_id: btn.dataset.playerId,
        map_id: btn.dataset.mapId,
        zone_type: btn.dataset.zoneType,
        zone_index: btn.dataset.zoneIndex,
        tf_class: btn.dataset.tfClass
    };

    fetch('/api/run/delete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded'},
        body: new URLSearchParams(params)
    })
        .then(async res => {
            const div = document.getElementById("player-run-form-response");
            const data = await res.json();
            div.textContent = data.message;
            div.style.padding = "4px";
            div.style.backgroundColor = res.ok ? "green" : "red";
            if (form_container) {
                form_container.style.outline = `${res.ok ? 'green' : 'red'} solid 2px`;
            }

            if (res.ok) {
                const row = btn.closest("tr");
                if (row) row.remove();
                if (data.stats) {
                    document.getElementById("otitle").innerText = data.stats.overall_title;
                    document.getElementById("orank").innerText = data.stats.overall_rank;
                    document.getElementById("opts").innerText = data.stats.overall_points.toFixed(2);
                    
                    document.getElementById("stitle").innerText = data.stats.soldier_title;
                    document.getElementById("srank").innerText = data.stats.soldier_rank;
                    document.getElementById("spts").innerText = data.stats.soldier_points.toFixed(2);
                    
                    document.getElementById("dtitle").innerText = data.stats.demoman_title;
                    document.getElementById("drank").innerText = data.stats.demoman_rank;
                    document.getElementById("dpts").innerText = data.stats.demoman_points.toFixed(2);
                }
            }
        })
        .catch(error => console.error(error));
});

function update_buttons() {
    btns.forEach((b) => {
        const btn = document.getElementById(b.column);
        btn.style.backgroundColor = b.column === current_button ? "#5f3699" : "#230e54";
        btn.children[0].innerHTML = b.direction >= 1 ? "arrow_drop_up" : "arrow_drop_down";
    })
}

function make_row(run) {
    const zoneType = run.zone_id.zone_type[0].toUpperCase() + run.zone_id.zone_type.substring(1);
    return `<tr>
                <td><a href="/maps/${run.zone_id.map_id}/${run.zone_id.zone_type}/${run.zone_id.zone_index}">${run.map_name}</a></td>
                <td>${run.zone_id.zone_type === 'map' ? 'Map Run' : zoneType + ' ' + run.zone_id.zone_index}</td>
                <td><img src="/${run.class_}.svg" style="display: flex; align-items: center" height="18px"></td>
                <td class="num">T${run.tier}</td>
                <td class="num">R${run.rating}</td>
                <td class="num">${formatDate(run.time_submitted)}</td>
                <td class="num">${formatDuration(run.duration)}</td>
                <td class="num _${run.group}">${run.rank}</td>
                <td class="num">${run.points.toFixed(2)}</td>
                        <td><button class="delete-run-btn" style="background: none; border: none;"
                        data-player-id="${run.player_id}"
                        data-map-id="${run.zone_id.map_id}"
                        data-zone-type="${run.zone_id.zone_type}"
                        data-zone-index="${run.zone_id.zone_index}"
                        data-tf-class="${run.class_}">
                        <img src="/trash.svg" width="10px">
                    </button></td>
            </tr>`;
}

async function update_table(append = false) {
    const res = await fetch(`/api/player/${player_id}/runs`, {
        method: "POST",
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({limit: append ? 100 : limit, offset: append ? offset : 0, orders: btns})
    });

    if (res.status === 200) {
        const data = await res.json();
        if (append) {
            data.map(run => table.insertAdjacentHTML("beforeend", make_row(run)))
        }
        else {
            table.innerHTML = data.map(run => make_row(run)).join("");
        }
    }
}

async function get_zone_counts() {
    let res = await fetch("/api/zone_counts");
    if (res.ok) {
        let select = document.getElementById("player-run-map-id");

        zone_counts = await res.json();
        zone_counts.forEach(row => select.insertAdjacentHTML("beforeend", `<option value="${row.map.id}">${row.map.name}</option>`));
        current_map = zone_counts[0];
    }
}

async function get_servers() {
    let res = await fetch("/api/servers");
    if (res.ok) {
        let select = document.getElementById("player-run-server");
        (await res.json()).forEach(row => select.insertAdjacentHTML("beforeend", `<option value="${row.server_id}">${row.server_name}</option>`));
    }
}