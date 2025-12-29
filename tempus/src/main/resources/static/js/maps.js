const sort_functions = {
    "btn-id": (a, b) => id_from_link(a.href) - id_from_link(b.href),
    "btn-name": (a, b) => a.querySelector("p").textContent.localeCompare(b.querySelector("p").textContent),
    "btn-tier": (a, b) => tier_from_map(a) - tier_from_map(b),
    "btn-rating": (a, b) => rating_from_map(a) - rating_from_map(b)
};

let sort_directions = {
    "btn-id": 1,
    "btn-name": 1,
    "btn-tier": -1,
    "btn-rating": 1
};

function max(a, b) {
    return a > b ? a : b;
}

function min(a, b) {
    return a < b ? a : b;
}

function id_from_link(link) {
    const parts = link.split("/");
    return Number(parts[parts.length - 3]);
}

function tier_from_map(map) {
    let class_info = map.querySelector(".flex-row").children;
    let stier = parseInt(class_info[0].querySelector("p").textContent.substring(1));
    let dtier = parseInt(class_info[1].querySelector("p").textContent.substring(1));
    return max(stier, dtier);
}

function rating_from_map(map) {
    let class_info = map.querySelector(".flex-row").children;
    let srating = parseInt(class_info[0].querySelectorAll("p")[1].textContent.substring(1));
    let drating = parseInt(class_info[1].querySelectorAll("p")[1].textContent.substring(1));
    return min(srating, drating);
}

let current_button = "btn-name";
const btns = ["btn-id", "btn-name", "btn-tier", "btn-rating"];

function update_buttons() {
    btns.forEach((id) => document.getElementById(id).style.backgroundColor = id === current_button ? "#5f3699" : "#230e54");
}

update_buttons();

btns.forEach((id) => {
    document.getElementById(id).addEventListener("click", (event) => {
        let btn = event.target.closest('button');
        if (current_button === id) {
            sort_directions[id] *= -1;
            btn.querySelector("span").textContent =  sort_directions[id] === 1 ? 'arrow_drop_up' : 'arrow_drop_down';
        }
        current_button = id
        update_buttons();
        let grid = document.getElementById("maps-grid");
        let maps = Array.from(grid.children);
        maps.sort((a, b) => sort_functions[id](a, b) * sort_directions[id]);
        grid.replaceChildren(...maps);
    });
});
