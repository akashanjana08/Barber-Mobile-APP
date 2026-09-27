const BarberShop = require('../models/BarberShop');
const Review = require('../models/Review');
const User = require('../models/User');
const { mapsService } = require('../services/thirdPartyService');

// @desc    Get all barber shops with filtering, search, and sorting
// @route   GET /api/v1/shops
exports.getAllShops = async (req, res, next) => {
  try {
    const {
      search,
      category,
      maxDistanceKm,
      minRating,
      maxPrice,
      openNow,
      availableToday,
      sortBy = 'RECOMMENDED',
      userLat,
      userLng,
    } = req.query;

    let query = {};

    // 1. Search Query
    if (search) {
      query.$or = [
        { name: { $regex: search, $options: 'i' } },
        { address: { $regex: search, $options: 'i' } },
        { city: { $regex: search, $options: 'i' } },
        { 'services.name': { $regex: search, $options: 'i' } },
      ];
    }

    // 2. Category filter
    if (category) {
      const categories = category.split(',').map((c) => c.trim());
      query['services.category'] = { $in: categories };
    }

    // 3. Minimum Rating
    if (minRating) {
      query.rating = { $gte: parseFloat(minRating) };
    }

    // 4. Maximum Price
    if (maxPrice) {
      query.startingPrice = { $lte: parseInt(maxPrice, 10) };
    }

    // 5. Open Now
    if (openNow === 'true') {
      query.isOpen = true;
    }

    let shops = await BarberShop.find(query);

    // Dynamic distance calculation if user coordinates supplied
    const currentLat = userLat ? parseFloat(userLat) : req.user?.location?.lat || 28.6500;
    const currentLng = userLng ? parseFloat(userLng) : req.user?.location?.lng || 77.4500;

    let userFavorites = new Set();
    if (req.user) {
      const user = await User.findById(req.user._id);
      if (user && user.favoriteShops) {
        userFavorites = new Set(user.favoriteShops);
      }
    }

    let processedShops = shops.map((shop) => {
      const shopObj = shop.toObject();
      const shopLat = shopObj.location?.coordinates ? shopObj.location.coordinates[1] : 28.6500;
      const shopLng = shopObj.location?.coordinates ? shopObj.location.coordinates[0] : 77.4500;
      const calculatedDistance = mapsService.calculateHaversineDistance(currentLat, currentLng, shopLat, shopLng);

      return {
        ...shopObj,
        distanceKm: calculatedDistance || shopObj.distanceKm,
        isFavorite: userFavorites.has(shopObj.shopId) || userFavorites.has(shopObj._id.toString()),
      };
    });

    // Distance filter
    if (maxDistanceKm) {
      const maxDist = parseFloat(maxDistanceKm);
      processedShops = processedShops.filter((s) => s.distanceKm <= maxDist);
    }

    // Available today filter
    if (availableToday === 'true') {
      processedShops = processedShops.filter((s) => !s.nextAvailableSlot.toLowerCase().includes('tomorrow'));
    }

    // Sorting
    switch (sortBy.toUpperCase()) {
      case 'NEAREST':
        processedShops.sort((a, b) => a.distanceKm - b.distanceKm);
        break;
      case 'HIGHEST_RATED':
        processedShops.sort((a, b) => b.rating - a.rating);
        break;
      case 'LOWEST_PRICE':
        processedShops.sort((a, b) => a.startingPrice - b.startingPrice);
        break;
      case 'RECOMMENDED':
      default:
        processedShops.sort((a, b) => b.rating * 10 - a.distanceKm * 2);
        break;
    }

    res.status(200).json({
      success: true,
      count: processedShops.length,
      data: processedShops,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Get single barber shop details
// @route   GET /api/v1/shops/:id
exports.getShopById = async (req, res, next) => {
  try {
    const shop = await BarberShop.findOne({
      $or: [{ shopId: req.params.id }, { _id: req.params.id.match(/^[0-9a-fA-F]{24}$/) ? req.params.id : null }],
    });

    if (!shop) {
      return res.status(404).json({ success: false, message: 'Shop not found' });
    }

    const shopObj = shop.toObject();

    let isFavorite = false;
    if (req.user) {
      const user = await User.findById(req.user._id);
      if (user && user.favoriteShops) {
        isFavorite = user.favoriteShops.includes(shopObj.shopId) || user.favoriteShops.includes(shopObj._id.toString());
      }
    }

    res.status(200).json({
      success: true,
      data: {
        ...shopObj,
        isFavorite,
      },
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Toggle favorite status for shop
// @route   POST /api/v1/shops/:id/favorite
exports.toggleFavorite = async (req, res, next) => {
  try {
    const user = await User.findById(req.user._id);
    if (!user) {
      return res.status(401).json({ success: false, message: 'User not authenticated' });
    }

    const shop = await BarberShop.findOne({
      $or: [{ shopId: req.params.id }, { _id: req.params.id.match(/^[0-9a-fA-F]{24}$/) ? req.params.id : null }],
    });

    if (!shop) {
      return res.status(404).json({ success: false, message: 'Shop not found' });
    }

    const targetShopId = shop.shopId;
    const isFav = user.favoriteShops.includes(targetShopId);

    if (isFav) {
      user.favoriteShops = user.favoriteShops.filter((id) => id !== targetShopId);
    } else {
      user.favoriteShops.push(targetShopId);
    }

    await user.save();

    res.status(200).json({
      success: true,
      isFavorite: !isFav,
      message: !isFav ? 'Added to favorites' : 'Removed from favorites',
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Get reviews for a shop
// @route   GET /api/v1/shops/:id/reviews
exports.getShopReviews = async (req, res, next) => {
  try {
    const reviews = await Review.find({ shopId: req.params.id }).sort({ createdAt: -1 });
    res.status(200).json({
      success: true,
      count: reviews.length,
      data: reviews,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Add review for a shop
// @route   POST /api/v1/shops/:id/reviews
exports.addShopReview = async (req, res, next) => {
  try {
    const { rating, comment, barberName } = req.body;
    if (!rating || !comment) {
      return res.status(400).json({ success: false, message: 'Please provide rating and comment' });
    }

    const reviewId = `REV_${Date.now()}`;
    const review = await Review.create({
      reviewId,
      shopId: req.params.id,
      userId: req.user?._id || null,
      userName: req.user?.name || 'Customer',
      barberName: barberName || '',
      rating: parseFloat(rating),
      comment,
      dateStr: 'Today',
    });

    // Update shop rating and review count
    const shop = await BarberShop.findOne({ shopId: req.params.id });
    if (shop) {
      shop.reviews.push({
        id: reviewId,
        userName: review.userName,
        rating: review.rating,
        comment: review.comment,
        date: 'Today',
        barberName: review.barberName,
      });
      shop.reviewCount += 1;
      shop.rating = parseFloat(((shop.rating * (shop.reviewCount - 1) + review.rating) / shop.reviewCount).toFixed(1));
      await shop.save();
    }

    res.status(201).json({
      success: true,
      message: 'Review submitted successfully',
      data: review,
    });
  } catch (error) {
    next(error);
  }
};

// @desc    Get available time slots for a shop and barber
// @route   GET /api/v1/shops/:id/slots
exports.getShopSlots = async (req, res, next) => {
  try {
    const { date, barberName } = req.query;

    const slots = {
      Morning: [
        { time: '10:00 AM', isAvailable: true },
        { time: '10:30 AM', isAvailable: true },
        { time: '11:00 AM', isAvailable: false },
        { time: '11:30 AM', isAvailable: true },
      ],
      Afternoon: [
        { time: '12:00 PM', isAvailable: false },
        { time: '12:30 PM', isAvailable: true },
        { time: '01:00 PM', isAvailable: true },
        { time: '02:00 PM', isAvailable: true },
        { time: '02:30 PM', isAvailable: false },
        { time: '03:00 PM', isAvailable: true },
      ],
      Evening: [
        { time: '04:30 PM', isAvailable: true },
        { time: '05:00 PM', isAvailable: false }, // Vikram Malhotra
        { time: '05:30 PM', isAvailable: true },
        { time: '06:00 PM', isAvailable: true },
        { time: '06:30 PM', isAvailable: false },
        { time: '07:00 PM', isAvailable: true },
        { time: '07:30 PM', isAvailable: true },
      ],
    };

    res.status(200).json({
      success: true,
      date: date || 'Today',
      barberName: barberName || 'Any Barber',
      data: slots,
    });
  } catch (error) {
    next(error);
  }
};
