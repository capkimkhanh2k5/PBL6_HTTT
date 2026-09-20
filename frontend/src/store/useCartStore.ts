import { create } from 'zustand';

export interface CartItem {
  id: string; // unique cart item id
  serviceId: string;
  slotId: string;
  quantity: number;
  unitPrice: number;
  // Denormalized data for UI display
  serviceName: string;
  thumbnailUrl: string;
  date: string;
  timeSlot: string;
  selectedDate?: string;
}

interface CartState {
  items: CartItem[];
  addItem: (item: Omit<CartItem, 'id'>) => void;
  removeItem: (id: string) => void;
  updateQuantity: (id: string, quantity: number) => void;
  clearCart: () => void;
  
  // Computed (getters could be here or just use standard selectors)
  getTotalItems: () => number;
  getTotalPrice: () => number;
}

export const useCartStore = create<CartState>((set, get) => ({
  items: [],
  
  addItem: (itemData) => set((state) => {
    // Check if item already exists (same service & slot)
    const existingItem = state.items.find(
      (item) => item.serviceId === itemData.serviceId && item.slotId === itemData.slotId
    );
    
    if (existingItem) {
      return {
        items: state.items.map((item) =>
          item.id === existingItem.id
            ? { ...item, quantity: item.quantity + itemData.quantity }
            : item
        )
      };
    }
    
    // Create new item
    const newItem = {
      ...itemData,
      id: Math.random().toString(36).substr(2, 9),
    };
    return { items: [...state.items, newItem] };
  }),
  
  removeItem: (id) => set((state) => ({
    items: state.items.filter((item) => item.id !== id)
  })),
  
  updateQuantity: (id, quantity) => set((state) => ({
    items: state.items.map((item) => 
      item.id === id ? { ...item, quantity } : item
    )
  })),
  
  clearCart: () => set({ items: [] }),
  
  getTotalItems: () => {
    return get().items.reduce((total, item) => total + item.quantity, 0);
  },
  
  getTotalPrice: () => {
    return get().items.reduce((total, item) => total + (item.quantity * item.unitPrice), 0);
  }
}));
