import create from 'zustand'

type CartItem = { id: number; name: string; qty: number; price: number }

type State = {
	items: CartItem[]
	add: (item: CartItem) => void
	remove: (id: number) => void
	updateQty: (id: number, qty: number) => void
	subtotal: () => number
}

export const useCart = create<State>((set, get) => ({
	items: [],
	add: (item) => set(state => {
		const exists = state.items.find(i => i.id === item.id)
		if (exists) return { items: state.items.map(i => i.id === item.id ? {...i, qty: i.qty + item.qty} : i) }
		return { items: [...state.items, item] }
	}),
	remove: (id) => set(state => ({ items: state.items.filter(i => i.id !== id) })),
	updateQty: (id, qty) => set(state => ({ items: state.items.map(i => i.id === id ? {...i, qty} : i) })),
	subtotal: () => get().items.reduce((s, i) => s + i.qty * i.price, 0),
}))
