package com.assan.finance.responseModel;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ResponseModelHomeScreenData {

    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private HomeData data;

    @SerializedName("sliderCount")
    private int sliderCount;

    @SerializedName("hasUiConfig")
    private boolean hasUiConfig;

    public boolean isSuccess() {
        return success;
    }

    public HomeData getData() {
        return data;
    }

    public int getSliderCount() {
        return sliderCount;
    }

    public boolean isHasUiConfig() {
        return hasUiConfig;
    }

    public static class HomeData {
        @SerializedName("sliderImages")
        private List<SliderImage> sliderImages;

        @SerializedName("uiConfig")
        private UiConfig uiConfig;

        public List<SliderImage> getSliderImages() {
            return sliderImages;
        }

        public UiConfig getUiConfig() {
            return uiConfig;
        }
    }

    public static class SliderImage {
        @SerializedName("imageId")
        private int imageId;

        @SerializedName("configId")
        private int configId;

        @SerializedName("imageUrl")
        private String imageUrl;

        @SerializedName("displayOrder")
        private int displayOrder;

        @SerializedName("isActive")
        private String isActive;

        @SerializedName("createdAt")
        private String createdAt;

        public int getImageId() {
            return imageId;
        }

        public int getConfigId() {
            return configId;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public int getDisplayOrder() {
            return displayOrder;
        }

        public String getIsActive() {
            return isActive;
        }

        public String getCreatedAt() {
            return createdAt;
        }
    }

    public static class UiConfig {
        @SerializedName("configId")
        private int configId;

        @SerializedName("marqueeText")
        private String marqueeText;

        @SerializedName("loanSchemeTitle")
        private String loanSchemeTitle;

        @SerializedName("loanSchemeDesc")
        private String loanSchemeDesc;

        @SerializedName("isActive")
        private String isActive;

        @SerializedName("lastUpdated")
        private String lastUpdated;

        public int getConfigId() {
            return configId;
        }

        public String getMarqueeText() {
            return marqueeText;
        }

        public String getLoanSchemeTitle() {
            return loanSchemeTitle;
        }

        public String getLoanSchemeDesc() {
            return loanSchemeDesc;
        }

        public String getIsActive() {
            return isActive;
        }

        public String getLastUpdated() {
            return lastUpdated;
        }
    }
}
