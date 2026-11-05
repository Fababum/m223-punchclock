const URL = '';
let entries = [];

let editingId = null;

const dateAndTimeToDate = (dateString, timeString) => {
    return `${dateString}T${timeString}`;
};

const startEdit = (entry) => {
    const form = document.querySelector('#createEntryForm');
    const [checkInDate, checkInTime] = entry.checkIn.split('T');
    const [checkOutDate, checkOutTime] = entry.checkOut.split('T');
    form.elements['checkInDate'].value = checkInDate;
    form.elements['checkInTime'].value = checkInTime.substring(0, 5);
    form.elements['checkOutDate'].value = checkOutDate;
    form.elements['checkOutTime'].value = checkOutTime.substring(0, 5);
    editingId = entry.id;
    form.elements['submit'].value = 'Update';
    document.querySelector('#cancelEdit').hidden = false;
};

const stopEdit = () => {
    const form = document.querySelector('#createEntryForm');
    editingId = null;
    form.reset();
    form.elements['submit'].value = 'Save';
    document.querySelector('#cancelEdit').hidden = true;
};

const updateEntry = (id, entry) => {
    fetch(`${URL}/entries/${id}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(entry)
    }).then((result) => {
        if (!result.ok) {
            return;
        }
        result.json().then((updated) => {
            entries = entries.map((e) => (e.id === updated.id ? updated : e));
            stopEdit();
            renderEntries();
        });
    });
};

const createEntry = (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    const entry = {};
    entry['checkIn'] = dateAndTimeToDate(formData.get('checkInDate'), formData.get('checkInTime'));
    entry['checkOut'] = dateAndTimeToDate(formData.get('checkOutDate'), formData.get('checkOutTime'));

    if (editingId !== null) {
        updateEntry(editingId, entry);
        return;
    }

    fetch(`${URL}/entries`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(entry)
    }).then((result) => {
        result.json().then((entry) => {
            entries.push(entry);
            renderEntries();
        });
    });
};

const indexEntries = () => {
    fetch(`${URL}/entries`, {
        method: 'GET'
    }).then((result) => {
        result.json().then((result) => {
            entries = result;
            renderEntries();
        });
    });
    renderEntries();
};

const deleteEntry = (id) => {
    fetch(`${URL}/entries/${id}`, {
        method: 'DELETE'
    }).then((result) => {
        if (result.ok || result.status === 404) {
            entries = entries.filter((entry) => entry.id !== id);
            if (editingId === id) {
                stopEdit();
            }
            renderEntries();
        }
    });
};

const createCell = (text) => {
    const cell = document.createElement('td');
    cell.innerText = text;
    return cell;
};

const renderEntries = () => {
    const display = document.querySelector('#entryDisplay');
    display.innerHTML = '';
    entries.forEach((entry) => {
        const row = document.createElement('tr');
        row.appendChild(createCell(entry.id));
        row.appendChild(createCell(new Date(entry.checkIn).toLocaleString()));
        row.appendChild(createCell(new Date(entry.checkOut).toLocaleString()));
        const actionCell = document.createElement('td');
        const editButton = document.createElement('button');
        editButton.type = 'button';
        editButton.innerText = 'Edit';
        editButton.addEventListener('click', () => startEdit(entry));
        actionCell.appendChild(editButton);
        const deleteButton = document.createElement('button');
        deleteButton.type = 'button';
        deleteButton.innerText = 'Delete';
        deleteButton.addEventListener('click', () => deleteEntry(entry.id));
        actionCell.appendChild(deleteButton);
        row.appendChild(actionCell);
        display.appendChild(row);
    });
};

document.addEventListener('DOMContentLoaded', function(){
    const createEntryForm = document.querySelector('#createEntryForm');
    createEntryForm.addEventListener('submit', createEntry);
    document.querySelector('#cancelEdit').addEventListener('click', stopEdit);
    indexEntries();
});