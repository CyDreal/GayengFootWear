package com.example.gayeng.ui.product;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.example.gayeng.R;
import com.example.gayeng.LoginRegisterActivity;
import com.example.gayeng.adapter.ProductImageAdapter;
import com.example.gayeng.api.ApiClient;
import com.example.gayeng.api.ApiService;
import com.example.gayeng.api.response.BaseResponse;
import com.example.gayeng.api.response.CartListResponse;
import com.example.gayeng.api.response.CartResponse;
import com.example.gayeng.api.response.ProductDetailResponse;
import com.example.gayeng.databinding.FragmentProductDetailBinding;
import com.example.gayeng.model.Cart;
import com.example.gayeng.model.CartItem;
import com.example.gayeng.model.Product;
import com.example.gayeng.model.ProductImage;
import com.example.gayeng.utils.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailFragment extends Fragment {
    private FragmentProductDetailBinding binding;
    private ApiService apiService;
    private int productId;
    private ProductImageAdapter imageAdapter;
    private boolean isDestroyed = false;
    private SessionManager sessionManager;
    private Call<?> activeCall; // Menangani error yang terjadi karena race condition antara network
                                // call dan lifecycle fragment. Saat user cepat kembali sebelum response selesai,
                                // binding sudah null tapi callback masih mencoba mengakses view.

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            productId = getArguments().getInt("product_id");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        binding = FragmentProductDetailBinding.inflate(inflater, container, false);
        sessionManager = new SessionManager(requireContext());

        // Hide bottom navigation
        requireActivity().findViewById(R.id.nav_view).setVisibility(View.GONE);

        isDestroyed = false;

        setupImageSlider();
        setupBackButton();
        loadProductDetail();
        updateViewCount();

        return binding.getRoot();
    }

    private void updateViewCount() {
        apiService = ApiClient.getClient();
        apiService.updateViewCount(productId).enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse> call, @NonNull Response<BaseResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Reload product details to get updated view count
                    loadProductDetail();
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse> call, @NonNull Throwable t) {
                // Handle error silently
            }
        });
    }

    private void setupImageSlider() {
        imageAdapter = new ProductImageAdapter(requireContext());
        binding.viewPagerImages.setAdapter(imageAdapter);
    }

    private void setupBackButton() {
        binding.backButton.setOnClickListener(v ->
            Navigation.findNavController(v).navigateUp()
        );
    }

    private void loadProductDetail() {
        if (isDestroyed) return;

        apiService = ApiClient.getClient();
        apiService.getProductDetail(productId).enqueue(new Callback<ProductDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProductDetailResponse> call,
                                   @NonNull Response<ProductDetailResponse> response) {
                if (isDestroyed || binding == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    Product product = response.body().getProduct();
                    displayProductDetails(product);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProductDetailResponse> call, @NonNull Throwable t) {
                if (isDestroyed || binding == null) return;
                // Handle error
            }
        });
    }

    private void displayProductDetails(Product product) {
        // Display main image (image_order = 0)
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            // Sort images based on image_order
            List<ProductImage> sortedImages = new ArrayList<>(product.getImages());
            Collections.sort(sortedImages, (a, b) -> Integer.compare(a.getImageOrder(), b.getImageOrder()));
            imageAdapter.setImages(product.getImages());
        }

        // Set text fields
        binding.tvProductName.setText(product.getProductName());
        binding.tvPrice.setText(String.format("Rp %,d", product.getPrice()));
        binding.tvCategory.setText(product.getCategory());
        binding.tvStock.setText(String.valueOf(product.getStock()));
        binding.tvVisitCount.setText(String.valueOf(product.getViewCount()));
        binding.tvDescription.setText(product.getDescription());
        // Update view count display
        binding.tvVisitCount.setText(String.valueOf(product.getViewCount()));
        // Add purchased quantity display
        binding.tvPurchased.setText(String.valueOf(product.getPurchaseQuantity()));

        // Handle product status
        boolean isAvailable = "available".equals(product.getStatus());

        // Update status chip
        binding.chipStatus.setText(product.getStatus());
        binding.chipStatus.setChipBackgroundColorResource(
                isAvailable ? R.color.success : R.color.error
        );

        // Update add to cart button state
        binding.btnAddToCart.setEnabled(isAvailable && product.getStock() > 0);
        binding.btnAddToCart.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(),
                        isAvailable ? R.color.primary : R.color.disabled)
        ));

        // Show/hide sold out text
        binding.tvSoldOut.setVisibility(isAvailable ? View.GONE : View.VISIBLE);

        // Setup add to cart functionality
        if (isAvailable && product.getStock() > 0) {
            setupAddToCart(product);
        }
    }

    private void setupAddToCart(Product product) {
        binding.btnAddToCart.setOnClickListener(v -> {
            if (sessionManager.isGuest()) {
                showLoginRequiredDialog();
                return;
            }

            if (product.getImages() == null || product.getImages().isEmpty()) {
                return;
            }

            // Show loading
            showLoading();

            // Create new CartItem
            CartItem newItem = new CartItem(
                    product.getId(),
                    product.getProductName(),
                    product.getPrice(),
                    1,
                    product.getImages().get(0).getImageUrl(),
                    product.getStock()
            );

            List<CartItem> currentCart = sessionManager.getCartItems();
            boolean productExists = false;
            int newQuantity = 1;

            // Check if product exists in cart and calculate new quantity
            for (CartItem existingItem : currentCart) {
                if (existingItem.getProductId() == product.getId()) {
                    newQuantity = existingItem.getQuantity() + 1;
                    if (newQuantity > product.getStock()) {
                        hideLoading();
                        showToast("Cannot exceed available stock");
                        return;
                    }
                    productExists = true;
                    break;
                }
            }

            // Update both SessionManager and Database
            syncCartUpdate(product.getId(), newQuantity, newItem, productExists);
        });
    }

    private void syncCartUpdate(int productId, int newQuantity, CartItem newItem, boolean productExists) {
        apiService.addToCart(
                sessionManager.getUserId(),
                productId,
                newQuantity
        ).enqueue(new Callback<CartResponse>() {
            @Override
            public void onResponse(@NonNull Call<CartResponse> call,
                                   @NonNull Response<CartResponse> response) {
                if (response.isSuccessful()) {
                    refreshUserCart();
                    if (productExists) {
                        showToast("Cart quantity updated");
                    } else {
                        showToast("Product added to cart");
                    }
                } else {
                    hideLoading();
                    showToast("Failed to update cart");
                }
            }

            @Override
            public void onFailure(@NonNull Call<CartResponse> call,
                                  @NonNull Throwable t) {
                hideLoading();
                showToast("Network error occurred");
            }
        });
    }

    private void refreshUserCart() {
        apiService.getUserCarts(sessionManager.getUserId())
                .enqueue(new Callback<CartListResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<CartListResponse> call,
                                           @NonNull Response<CartListResponse> response) {
                        hideLoading();
                        if (response.isSuccessful() && response.body() != null) {
                            updateSessionManagerCart(response.body().getCarts());
                        } else {
                            showToast("Failed to refresh cart");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<CartListResponse> call,
                                          @NonNull Throwable t) {
                        hideLoading();
                        showToast("Network error occurred");
                    }
                });
    }
    private void updateSessionManagerCart(List<Cart> carts) {
        // Clear existing cart in SessionManager
        sessionManager.clearCart();

        // Add new items from API response
        for (Cart cart : carts) {
            CartItem item = cart.toCartItem();
            if (item != null) {
                sessionManager.addToCart(item);
            }
        }
    }

    private void showLoading() {
        if (binding != null) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnAddToCart.setEnabled(false);
        }
    }

    private void hideLoading() {
        if (binding != null && !isDestroyed) {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnAddToCart.setEnabled(true);
        }
    }

    private void showToast(String message) {
        if (getContext() != null && !isDestroyed) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoginRequiredDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Login Required")
                .setMessage("You need to login to add items to cart")
                .setPositiveButton("Login", (dialog, which) -> {
                    Intent intent = new Intent(requireContext(), LoginRegisterActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isDestroyed = true;
        // Kembalikan visibility bottom navigation saat fragment dihancurkan
        if (getActivity() != null) {
            getActivity().findViewById(R.id.nav_view).setVisibility(View.VISIBLE);
        }
        binding = null;
    }
}