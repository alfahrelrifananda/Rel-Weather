package com.example.relweather;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "WeatherAppPrefs";
    private static final String KEY_DEFAULT_CITY = "default_city";
    private static final String KEY_LANGUAGE = "language";
    private TextView cityNameText, temperatureText, descriptionText, dateText, feelsLikeText;
    private TextView humidityText, windSpeedText;
    private TextView pressureValue, visibilityValue, uvValue;
    private ImageView weatherIcon;
    private MaterialCardView welcomeCard, dateCard;
    private MaterialCardView humidityWindCard, pressureCard, visibilityCard, uvCard;
    private FloatingActionButton setDefaultCityfab;
    private FloatingActionButton fab;
    private MaterialCardView searchCard;
    private EditText searchEditText;
    private ImageButton searchBackButton, searchClearButton;
    private View searchOverlay;
    private ScrollView scrollView;
    private JSONObject currentWeatherData;
    private String currentCityName = "";
    private SharedPreferences sharedPreferences;
    private boolean isFabVisible = true;
    private int lastScrollY = 0;
    private final String[] languageCodes = {"en", "id"};
    private final String[] languageNames = {
            "English", "Bahasa Indonesia"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        applySavedLanguage();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            return insets;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (searchCard.getVisibility() == View.VISIBLE) {
                    hideSearchView();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        initializeViews();
        setupClickListeners();
        setupSearchView();
        setupScrollListener();

        String defaultCity = sharedPreferences.getString(KEY_DEFAULT_CITY, "Surakarta");
        new WeatherTask().execute(defaultCity);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedLanguage = prefs.getString(KEY_LANGUAGE, "en");

        Locale locale = new Locale(savedLanguage);
        Configuration config = new Configuration(newBase.getResources().getConfiguration());
        config.setLocale(locale);

        Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }

    private void applySavedLanguage() {
        String savedLanguage = sharedPreferences.getString(KEY_LANGUAGE, "en");
        Locale locale = new Locale(savedLanguage);
        Locale.setDefault(locale);

        Configuration config = new Configuration(getResources().getConfiguration());
        config.setLocale(locale);

        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private void initializeViews() {
        cityNameText = findViewById(R.id.cityNameText);
        temperatureText = findViewById(R.id.temperatureText);
        descriptionText = findViewById(R.id.descriptionText);
        dateText = findViewById(R.id.dateText);
        feelsLikeText = findViewById(R.id.feelsLikeText);
        weatherIcon = findViewById(R.id.weatherIcon);

        humidityText = findViewById(R.id.humidityText);
        windSpeedText = findViewById(R.id.windSpeedText);
        pressureValue = findViewById(R.id.pressure_value);
        visibilityValue = findViewById(R.id.visibility_value);
        uvValue = findViewById(R.id.uv_value);

        welcomeCard = findViewById(R.id.welcome_card);
        humidityWindCard = findViewById(R.id.humidity_wind_card);
        pressureCard = findViewById(R.id.pressure_card);
        visibilityCard = findViewById(R.id.visibility_card);
        uvCard = findViewById(R.id.uv_card);

        fab = findViewById(R.id.fab);
        scrollView = findViewById(R.id.scrollView);

        searchCard = findViewById(R.id.search_card);
        searchEditText = findViewById(R.id.search_edit_text);
        searchBackButton = findViewById(R.id.search_back_button);
        searchClearButton = findViewById(R.id.search_clear_button);
        searchOverlay = findViewById(R.id.search_overlay);

        SimpleDateFormat sdf = new SimpleDateFormat(getString(R.string.date_format), Locale.getDefault());
        dateText.setText(sdf.format(new Date()));

        setDefaultCityfab = findViewById(R.id.set_default_city_fab);
    }

    private void setupScrollListener() {
        scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                int scrollY = scrollView.getScrollY();

                if (searchCard.getVisibility() != View.VISIBLE) {
                    if (scrollY > lastScrollY && scrollY > 100) {
                        hideFab();
                    } else if (scrollY < lastScrollY || scrollY <= 100) {
                        showFab();
                    }
                }

                lastScrollY = scrollY;
            }
        });
    }

    private void hideFab() {
        if (isFabVisible) {
            isFabVisible = false;
            fab.animate()
                    .scaleX(0f)
                    .scaleY(0f)
                    .setDuration(200)
                    .start();

            setDefaultCityfab.animate()
                    .scaleX(0f)
                    .scaleY(0f)
                    .setDuration(200)
                    .start();
        }
    }

    private void showFab() {
        if (!isFabVisible) {
            isFabVisible = true;
            fab.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start();

            setDefaultCityfab.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start();
        }
    }

    private void setupClickListeners() {
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSearchView();
            }
        });

        setDefaultCityfab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurrentCityAsDefault();
            }
        });

        humidityWindCard.setOnClickListener(v -> showWeatherDetailsBottomSheet());
        pressureCard.setOnClickListener(v -> showWeatherDetailsBottomSheet());
        visibilityCard.setOnClickListener(v -> showWeatherDetailsBottomSheet());
        uvCard.setOnClickListener(v -> showWeatherDetailsBottomSheet());

        welcomeCard.setOnLongClickListener(v -> {
            showSettingsDialog();
            return true;
        });

        cityNameText.setOnLongClickListener(v -> {
            showCityOptionsDialog();
            return true;
        });
    }

    private void setCurrentCityAsDefault() {
        if (currentCityName.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_city_loaded), Toast.LENGTH_SHORT).show();
            return;
        }

        sharedPreferences.edit()
                .putString(KEY_DEFAULT_CITY, currentCityName)
                .apply();

        setDefaultCityfab.setImageResource(R.drawable.ic_baseline_location_pin_24);

        Snackbar.make(findViewById(R.id.main),
                        getString(R.string.default_city_set, currentCityName),
                        Snackbar.LENGTH_LONG)
                .show();

        setDefaultCityfab.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(150)
                .withEndAction(() -> {
                    setDefaultCityfab.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                            .start();
                })
                .start();
    }

    private void updateDefaultCityIcon() {
        String defaultCity = sharedPreferences.getString(KEY_DEFAULT_CITY, "Surakarta");

        if (currentCityName.equalsIgnoreCase(defaultCity)) {
            setDefaultCityfab.setImageResource(R.drawable.ic_baseline_location_pin_24);
        } else {
            setDefaultCityfab.setImageResource(R.drawable.ic_outline_location_on_24);
        }
    }

    private void showSettingsDialog() {
        String[] options = {
                getString(R.string.change_language),
                getString(R.string.set_default_city)
        };

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(getString(R.string.settings))
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showLanguageDialog();
                            break;
                        case 1:
                            showSetDefaultCityDialog();
                            break;
                    }
                })
                .show();
    }

    private void showCityOptionsDialog() {
        if (currentCityName.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_city_loaded), Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {
                getString(R.string.mark_as_default_city),
                getString(R.string.change_language)
        };

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(getString(R.string.city_options))
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            setAsDefaultCity(currentCityName);
                            break;
                        case 1:
                            showLanguageDialog();
                            break;
                    }
                })
                .show();
    }

    private void showLanguageDialog() {
        String currentLanguage = sharedPreferences.getString(KEY_LANGUAGE, "en");
        int selectedIndex = 0;

        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(currentLanguage)) {
                selectedIndex = i;
                break;
            }
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(getString(R.string.select_language))
                .setSingleChoiceItems(languageNames, selectedIndex, (dialog, which) -> {
                    String selectedLanguage = languageCodes[which];

                    if (!selectedLanguage.equals(currentLanguage)) {
                        sharedPreferences.edit()
                                .putString(KEY_LANGUAGE, selectedLanguage)
                                .apply();

                        dialog.dismiss();
                        recreate();
                    } else {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showSetDefaultCityDialog() {
        final EditText input = new EditText(this);
        input.setHint(getString(R.string.enter_city_name));

        String currentDefault = sharedPreferences.getString(KEY_DEFAULT_CITY, "Surakarta");
        input.setText(currentDefault);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(getString(R.string.set_default_city))
                .setView(input)
                .setPositiveButton(getString(R.string.save), (dialog, which) -> {
                    String cityName = input.getText().toString().trim();
                    if (!cityName.isEmpty()) {
                        setAsDefaultCity(cityName);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void setAsDefaultCity(String cityName) {
        sharedPreferences.edit()
                .putString(KEY_DEFAULT_CITY, cityName)
                .apply();

        Snackbar.make(findViewById(R.id.main),
                        getString(R.string.default_city_set, cityName),
                        Snackbar.LENGTH_LONG)
                .show();
    }

    private void setupSearchView() {
        searchCard.setVisibility(View.GONE);
        searchOverlay.setVisibility(View.GONE);
        searchBackButton.setOnClickListener(v -> hideSearchView());
        searchClearButton.setOnClickListener(v -> {
            searchEditText.setText("");
            searchClearButton.setVisibility(View.GONE);
        });

        searchOverlay.setOnClickListener(v -> hideSearchView());

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    searchClearButton.setVisibility(View.VISIBLE);
                } else {
                    searchClearButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            String query = searchEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                performSearch(query);
                hideSearchView();
                return true;
            }
            return false;
        });
    }

    private void showSearchView() {
        searchOverlay.setVisibility(View.VISIBLE);
        searchOverlay.setAlpha(0f);
        searchOverlay.animate()
                .alpha(1f)
                .setDuration(250)
                .start();

        searchCard.setVisibility(View.VISIBLE);
        searchCard.setTranslationY(-searchCard.getHeight());
        searchCard.animate()
                .translationY(0f)
                .setDuration(300)
                .start();

        searchEditText.requestFocus();
        searchEditText.postDelayed(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getWindow().getDecorView().getWindowInsetsController()
                        .show(WindowInsets.Type.ime());
            }
        }, 100);

        fab.animate()
                .scaleX(0f)
                .scaleY(0f)
                .setDuration(200)
                .start();
    }

    private void hideSearchView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().getDecorView().getWindowInsetsController()
                    .hide(WindowInsets.Type.ime());
        }

        searchCard.animate()
                .translationY(-searchCard.getHeight())
                .setDuration(250)
                .withEndAction(() -> {
                    searchCard.setVisibility(View.GONE);
                    searchEditText.setText("");
                    searchClearButton.setVisibility(View.GONE);
                })
                .start();

        searchOverlay.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> searchOverlay.setVisibility(View.GONE))
                .start();

        fab.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(250)
                .setStartDelay(100)
                .start();
    }

    private void performSearch(String query) {
        new WeatherTask().execute(query);
        Toast.makeText(this, getString(R.string.searching_for, query), Toast.LENGTH_SHORT).show();
    }

    private void showWeatherDetailsBottomSheet() {
        if (currentWeatherData == null) {
            Toast.makeText(this, getString(R.string.weather_data_not_available), Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);

        bottomSheetDialog.setCancelable(true);
        bottomSheetDialog.setCanceledOnTouchOutside(true);

        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_weather_details, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        Button closeBottomSheetBtn = bottomSheetView.findViewById(R.id.bottom_sheet_close_btn);
        FloatingActionButton closeBottomSheetFab = bottomSheetView.findViewById(R.id.close_button);

        try {
            JSONObject main = currentWeatherData.getJSONObject("main");
            JSONObject wind = currentWeatherData.getJSONObject("wind");
            JSONObject weather = currentWeatherData.getJSONArray("weather").getJSONObject(0);
            JSONObject sys = currentWeatherData.getJSONObject("sys");

            TextView detailCityName = bottomSheetView.findViewById(R.id.detail_city_name);
            TextView detailTemperature = bottomSheetView.findViewById(R.id.detail_temperature);
            TextView detailDescription = bottomSheetView.findViewById(R.id.detail_description);
            TextView detailFeelsLike = bottomSheetView.findViewById(R.id.detail_feels_like);
            TextView detailHumidity = bottomSheetView.findViewById(R.id.detail_humidity);
            TextView detailPressure = bottomSheetView.findViewById(R.id.detail_pressure);
            TextView detailWindSpeed = bottomSheetView.findViewById(R.id.detail_wind_speed);
            TextView detailWindDirection = bottomSheetView.findViewById(R.id.detail_wind_direction);
            TextView detailVisibility = bottomSheetView.findViewById(R.id.visibility_value);
            TextView detailCloudiness = bottomSheetView.findViewById(R.id.cloudiness_value);
            TextView detailSunrise = bottomSheetView.findViewById(R.id.detail_sunrise);
            TextView detailSunset = bottomSheetView.findViewById(R.id.detail_sunset);
            TextView detailMinTemp = bottomSheetView.findViewById(R.id.detail_min_temp);
            TextView detailMaxTemp = bottomSheetView.findViewById(R.id.detail_max_temp);
            ImageView detailWeatherIcon = bottomSheetView.findViewById(R.id.detail_weather_icon);

            String cityName = currentWeatherData.getString("name");
            String country = sys.getString("country");
            detailCityName.setText(getString(R.string.city_country_format, cityName, country));

            double temperature = main.getDouble("temp");
            detailTemperature.setText(getString(R.string.temperature_celsius, Math.round(temperature)));

            String description = weather.getString("description");
            detailDescription.setText(capitalizeWords(description));

            double feelsLike = main.getDouble("feels_like");
            detailFeelsLike.setText(getString(R.string.feels_like_temperature, Math.round(feelsLike)));

            int humidity = main.getInt("humidity");
            detailHumidity.setText(getString(R.string.humidity_percentage, humidity));

            double pressure = main.getDouble("pressure");
            detailPressure.setText(getString(R.string.pressure_hpa, Math.round(pressure)));

            double windSpeed = wind.getDouble("speed");
            detailWindSpeed.setText(getString(R.string.wind_speed_ms, windSpeed));

            if (wind.has("deg")) {
                int windDeg = wind.getInt("deg");
                String direction = getWindDirection(windDeg);
                detailWindDirection.setText(getString(R.string.wind_direction_degrees, direction, windDeg));
            } else {
                detailWindDirection.setText(getString(R.string.not_available));
            }

            double visibility = currentWeatherData.optDouble("visibility", 10000) / 1000.0;
            detailVisibility.setText(getString(R.string.visibility_km, visibility));

            if (currentWeatherData.has("clouds")) {
                int cloudiness = currentWeatherData.getJSONObject("clouds").getInt("all");
                detailCloudiness.setText(getString(R.string.cloudiness_percentage, cloudiness));
            } else {
                detailCloudiness.setText(getString(R.string.default_cloudiness));
            }

            double minTemp = main.getDouble("temp_min");
            double maxTemp = main.getDouble("temp_max");
            detailMinTemp.setText(getString(R.string.temperature_celsius, Math.round(minTemp)));
            detailMaxTemp.setText(getString(R.string.temperature_celsius, Math.round(maxTemp)));

            long sunrise = sys.getLong("sunrise") * 1000;
            long sunset = sys.getLong("sunset") * 1000;
            SimpleDateFormat timeFormat = new SimpleDateFormat(getString(R.string.time_format), Locale.getDefault());
            detailSunrise.setText(timeFormat.format(new Date(sunrise)));
            detailSunset.setText(timeFormat.format(new Date(sunset)));

            String weatherMain = weather.getString("main");
            setDetailWeatherIcon(detailWeatherIcon, weatherMain);

            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View) bottomSheetView.getParent());
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setHideable(true);
            behavior.setDraggable(true);

            behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        bottomSheetDialog.dismiss();
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                }
            });

            closeBottomSheetBtn.setOnClickListener(v -> bottomSheetDialog.dismiss());
            closeBottomSheetFab.setOnClickListener(v -> bottomSheetDialog.dismiss());

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.error_loading_weather_details), Toast.LENGTH_SHORT).show();
            return;
        }

        bottomSheetDialog.show();
    }

    private String getWindDirection(int degrees) {
        String[] directions = {
                getString(R.string.wind_direction_n),
                getString(R.string.wind_direction_nne),
                getString(R.string.wind_direction_ne),
                getString(R.string.wind_direction_ene),
                getString(R.string.wind_direction_e),
                getString(R.string.wind_direction_ese),
                getString(R.string.wind_direction_se),
                getString(R.string.wind_direction_sse),
                getString(R.string.wind_direction_s),
                getString(R.string.wind_direction_ssw),
                getString(R.string.wind_direction_sw),
                getString(R.string.wind_direction_wsw),
                getString(R.string.wind_direction_w),
                getString(R.string.wind_direction_wnw),
                getString(R.string.wind_direction_nw),
                getString(R.string.wind_direction_nnw)
        };
        int index = (int) Math.round(degrees / 22.5) % 16;
        return directions[index];
    }

    private void setDetailWeatherIcon(ImageView iconView, String weatherMain) {
        String weatherCondition = weatherMain.toLowerCase();
        if (weatherCondition.equals(getString(R.string.weather_clear))) {
            iconView.setImageResource(R.drawable.ic_outline_sunny_24);
        } else if (weatherCondition.equals(getString(R.string.weather_clouds))) {
            iconView.setImageResource(R.drawable.ic_outline_cloud_24);
        } else if (weatherCondition.equals(getString(R.string.weather_rain))) {
            iconView.setImageResource(R.drawable.ic_outline_rainy_24);
        } else if (weatherCondition.equals(getString(R.string.weather_snow))) {
            iconView.setImageResource(R.drawable.ic_outline_snowing_24);
        } else if (weatherCondition.equals(getString(R.string.weather_thunderstorm))) {
            iconView.setImageResource(R.drawable.ic_outline_thunderstorm_24);
        } else if (weatherCondition.equals(getString(R.string.weather_drizzle))) {
            iconView.setImageResource(R.drawable.ic_outline_rainy_light_24);
        } else if (weatherCondition.equals(getString(R.string.weather_mist)) ||
                weatherCondition.equals(getString(R.string.weather_fog)) ||
                weatherCondition.equals(getString(R.string.weather_haze))) {
            iconView.setImageResource(R.drawable.ic_outline_foggy_24);
        } else {
            iconView.setImageResource(R.drawable.ic_outline_sunny_24);
        }
    }

    private class WeatherTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            fab.animate().rotation(360f).setDuration(1000);
        }

        @Override
        protected String doInBackground(String... params) {
            String cityName = params[0];
            try {
                String apiUrl = getString(R.string.api_base_url) + cityName +
                        getString(R.string.api_key_param) + BuildConfig.API_KEY +
                        getString(R.string.api_units_param);
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                connection.disconnect();

                return response.toString();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            fab.animate().rotation(0f).setDuration(300);

            if (result != null) {
                try {
                    JSONObject jsonObject = new JSONObject(result);

                    if (jsonObject.has("cod") && jsonObject.getInt("cod") == 200) {
                        currentWeatherData = jsonObject;

                        String cityName = jsonObject.getString("name");
                        currentCityName = cityName;
                        updateDefaultCityIcon();
                        String country = jsonObject.getJSONObject("sys").getString("country");

                        JSONObject main = jsonObject.getJSONObject("main");
                        double temperature = main.getDouble("temp");
                        double feelsLike = main.getDouble("feels_like");
                        int humidity = main.getInt("humidity");
                        double pressure = main.getDouble("pressure");

                        JSONObject weather = jsonObject.getJSONArray("weather").getJSONObject(0);
                        String description = weather.getString("description");
                        String weatherMain = weather.getString("main");

                        JSONObject wind = jsonObject.getJSONObject("wind");
                        double windSpeed = wind.getDouble("speed");

                        double visibility = jsonObject.optDouble("visibility", 10000) / 1000.0;

                        cityNameText.setText(getString(R.string.city_country_format, cityName, country));
                        temperatureText.setText(getString(R.string.temperature_celsius, Math.round(temperature)));
                        descriptionText.setText(capitalizeWords(description));
                        feelsLikeText.setText(getString(R.string.feels_like_temperature, Math.round(feelsLike)));

                        humidityText.setText(getString(R.string.humidity_percentage, humidity));
                        windSpeedText.setText(getString(R.string.wind_speed_ms, windSpeed));
                        pressureValue.setText(getString(R.string.pressure_hpa, Math.round(pressure)));
                        visibilityValue.setText(getString(R.string.visibility_km, visibility));

                        String uvIndex = calculateUVIndex(weatherMain);
                        uvValue.setText(uvIndex);

                        setWeatherIcon(weatherMain);
                        animateCards();

                    } else {
                        Toast.makeText(MainActivity.this, getString(R.string.city_not_found), Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, getString(R.string.error_parsing_weather_data), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, getString(R.string.error_fetching_weather_data), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void animateCards() {
        welcomeCard.setAlpha(0f);
        humidityWindCard.setAlpha(0f);
        pressureCard.setAlpha(0f);
        visibilityCard.setAlpha(0f);
        uvCard.setAlpha(0f);

        welcomeCard.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(100);

        humidityWindCard.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(200);

        pressureCard.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(300);

        visibilityCard.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(400);

        uvCard.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(500);
    }

    private void setWeatherIcon(String weatherMain) {
        String weatherCondition = weatherMain.toLowerCase();
        if (weatherCondition.equals(getString(R.string.weather_clear))) {
            weatherIcon.setImageResource(R.drawable.ic_outline_sunny_24);
        } else if (weatherCondition.equals(getString(R.string.weather_clouds))) {
            weatherIcon.setImageResource(R.drawable.ic_outline_cloud_24);
        } else if (weatherCondition.equals(getString(R.string.weather_rain))) {
            weatherIcon.setImageResource(R.drawable.ic_outline_rainy_24);
        } else if (weatherCondition.equals(getString(R.string.weather_snow))) {
            weatherIcon.setImageResource(R.drawable.ic_outline_snowing_24);
        } else if (weatherCondition.equals(getString(R.string.weather_thunderstorm))) {
            weatherIcon.setImageResource(R.drawable.ic_outline_thunderstorm_24);
        } else if (weatherCondition.equals(getString(R.string.weather_drizzle))) {
            weatherIcon.setImageResource(R.drawable.ic_outline_rainy_light_24);
        } else if (weatherCondition.equals(getString(R.string.weather_mist)) ||
                weatherCondition.equals(getString(R.string.weather_fog)) ||
                weatherCondition.equals(getString(R.string.weather_haze))) {
            weatherIcon.setImageResource(R.drawable.ic_outline_foggy_24);
        } else {
            weatherIcon.setImageResource(R.drawable.ic_outline_sunny_24);
        }
    }

    private String calculateUVIndex(String weatherMain) {
        String weatherCondition = weatherMain.toLowerCase();

        if (weatherCondition.equals(getString(R.string.weather_clear))) {
            return getString(R.string.uv_index_high);
        } else if (weatherCondition.equals(getString(R.string.weather_clouds))) {
            return getString(R.string.uv_index_moderate_4);
        } else if (weatherCondition.equals(getString(R.string.weather_rain)) ||
                weatherCondition.equals(getString(R.string.weather_thunderstorm)) ||
                weatherCondition.equals(getString(R.string.weather_drizzle))) {
            return getString(R.string.uv_index_low_2);
        } else if (weatherCondition.equals(getString(R.string.weather_snow))) {
            return getString(R.string.uv_index_moderate_3);
        } else if (weatherCondition.equals(getString(R.string.weather_mist)) ||
                weatherCondition.equals(getString(R.string.weather_fog)) ||
                weatherCondition.equals(getString(R.string.weather_haze))) {
            return getString(R.string.uv_index_low_1);
        } else {
            return getString(R.string.uv_index_moderate_5);
        }
    }

    private String capitalizeWords(String text) {
        String[] words = text.split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
                result.append(" ");
            }
        }

        return result.toString().trim();
    }
}